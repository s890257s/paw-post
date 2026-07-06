package tw.pers.allen.backend.core.init;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 開發用「拋棄式資料庫」重置器:在 Spring 啟動前,整顆 DROP 掉再重建,
 * 讓每次啟動都像 in-memory 資料庫一樣從乾淨狀態開始
 * (之後由 spring.sql.init 執行 init/schema、init/data 重建資料)。
 *
 * ⚠⚠⚠ 正式專案絕對禁止使用這個技巧,原因有三 ⚠⚠⚠
 * 1. 正式資料庫裡的資料就是公司資產,「啟動即刪庫」等於啟動即毀損資料。
 * 2. 最小權限原則:應用程式帳號在正式環境根本不該擁有 DROP DATABASE 權限。
 * 3. 正式環境的資料表演進靠 migration 工具(如 Flyway),不靠砍掉重練。
 *
 * 其他已知限制:
 * - 這裡用 java.util.Properties「手動」讀 application.properties,
 *   因為此時 Spring 還沒啟動。代價是 Spring 的 profile、環境變數、
 *   命令列參數等覆寫機制在這裡通通不生效——只認 application.properties 本尊。
 * - 因為專案掛了 devtools,main 在初次啟動會執行兩次、之後每次存檔熱重啟
 *   都會再執行一次(= 資料庫再次清空)。重置本身是冪等的,多跑只是慢一點。
 */
public class DatabaseResetter {

	private static final Logger log = LoggerFactory.getLogger(DatabaseResetter.class);

	/**
	 * 讀取連線設定並重置資料庫。
	 *
	 * @return 是否真的做了重置(main 據此決定要不要執行 init 腳本)
	 */
	public static boolean run() {
		try {
			Properties props = loadApplicationProperties();

			if (!Boolean.parseBoolean(props.getProperty("app.db.reset"))) {
				log.info("app.db.reset=false,跳過資料庫重置(保留既有資料)。");
				return false;
			}

			String url = props.getProperty("spring.datasource.url");
			String username = props.getProperty("spring.datasource.username");
			String password = props.getProperty("spring.datasource.password");

			// 例:jdbc:sqlserver://localhost:1433;databaseName=pawpostdb;encrypt=true;...
			String host = parseHost(url);
			String dbName = parseDatabaseName(url);

			// 防禦性檢查:重置只允許對「本機」資料庫執行。
			// 沒有這一道,連線字串哪天被改指向共用或正式伺服器,啟動就是刪庫。
			if (!host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1")) {
				throw new IllegalStateException(
						"資料庫重置僅允許本機 (localhost),目前連線目標為 [%s]。若你確定要連遠端,請先把 app.db.reset 改為 false。"
								.formatted(host));
			}

			// 不能 DROP 自己正連著的資料庫(而且第一次啟動時它根本還不存在),
			// 所以改連到系統資料庫 master,在 master 上執行 DROP / CREATE。
			String masterUrl = url.replace("databaseName=" + dbName, "databaseName=master");

			try (Connection conn = DriverManager.getConnection(masterUrl, username, password);
					Statement stmt = conn.createStatement()) {

				// 先把佔用中的連線全部踢掉(例如開著的 SSMS 查詢視窗),否則 DROP 會失敗。
				// SINGLE_USER + ROLLBACK IMMEDIATE 同樣是正式環境的禁忌動作。
				stmt.execute("""
						IF DB_ID('%s') IS NOT NULL
						    ALTER DATABASE [%s] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
						""".formatted(dbName, dbName));
				stmt.execute("DROP DATABASE IF EXISTS [%s];".formatted(dbName));
				stmt.execute("CREATE DATABASE [%s];".formatted(dbName));
			}

			log.info("資料庫 [{}] 已重置完成(DROP 後重建)。", dbName);
			return true;

		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(
					"資料庫重置失敗。請確認:1) SQL Server 服務已啟動 2) TCP/IP 已啟用且 port 正確 3) sa 帳密正確(見 init/README.md 安裝雷點)。",
					e);
		}
	}

	private static Properties loadApplicationProperties() throws Exception {
		Properties props = new Properties();
		try (InputStream in = DatabaseResetter.class.getResourceAsStream("/application.properties")) {
			// 指定 UTF-8:設定檔裡有中文註解,用預設編碼讀會變亂碼
			props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		return props;
	}

	// 取出 jdbc:sqlserver://「host」:port;... 的 host 部分
	// (相容 localhost\SQLEXPRESS 這種具名執行個體寫法)
	private static String parseHost(String url) {
		String afterSlashes = url.substring(url.indexOf("//") + 2);
		String hostPart = afterSlashes.split(";")[0];
		return hostPart.split("[:\\\\]")[0];
	}

	// 取出 databaseName=「xxx」 的資料庫名稱
	private static String parseDatabaseName(String url) {
		for (String param : url.split(";")) {
			if (param.startsWith("databaseName=")) {
				return param.substring("databaseName=".length());
			}
		}
		throw new IllegalStateException("連線字串裡找不到 databaseName 參數:" + url);
	}

}
