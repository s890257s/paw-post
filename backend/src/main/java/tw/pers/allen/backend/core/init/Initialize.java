package tw.pers.allen.backend.core.init;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.model.entity.Post;
import tw.pers.allen.backend.repository.PostRepository;

// 應用啟動後執行：為 data.sql 建立的種子貼文載入示範照片
// (照片檔以貼文 id 命名，放在 resources/init/photo/ 下)
@Component
@RequiredArgsConstructor
public class Initialize implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Initialize.class);

    private final PostRepository postRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initPhoto();
    }

    private void initPhoto() throws IOException {

        List<Post> posts = postRepository.findAll();

        for (Post p : posts) {
            ClassPathResource resource = new ClassPathResource("init/photo/%s.webp".formatted(p.getId()));

            // 找不到對應照片就跳過，不讓整個應用啟動失敗
            // (例如學生在 data.sql 加了新貼文，卻沒有放對應的照片檔)
            if (!resource.exists()) {
                log.warn("找不到貼文 {} 的示範照片 (init/photo/{}.webp)，略過。", p.getId(), p.getId());
                continue;
            }

            byte[] photo = resource.getContentAsByteArray();
            p.setImageData(photo);
            // 示範照片是 webp 格式，MIME type 必須與實際內容一致
            p.setImageContentType("image/webp");
        }

        postRepository.saveAll(posts);
    }

}
