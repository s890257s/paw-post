package tw.pers.allen.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import tw.pers.allen.backend.core.init.DatabaseResetter;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// 必須在 Spring 啟動「之前」重置:連線池一旦連上,資料庫就無法 DROP
		boolean didReset = DatabaseResetter.run();

		// 有重置、資料庫是全新空庫,才執行 init 的 schema/data 腳本;
		// 沒重置卻重跑腳本,會對既有資料庫重複建表導致啟動失敗
		System.setProperty("spring.sql.init.mode", didReset ? "always" : "never");

		SpringApplication.run(BackendApplication.class, args);
	}

}
