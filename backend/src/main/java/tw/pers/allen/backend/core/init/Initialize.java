package tw.pers.allen.backend.core.init;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.pers.allen.backend.model.entity.Post;
import tw.pers.allen.backend.repository.PostRepository;

@Component
@RequiredArgsConstructor
public class Initialize implements ApplicationRunner {

    private final PostRepository postRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initPhoto();
    }

    private void initPhoto() throws IOException {

        List<Post> posts = postRepository.findAll();

        for (Post p : posts) {
            ClassPathResource resource = new ClassPathResource("init/photo/%s.webp".formatted(p.getId()));
            byte[] photo = resource.getContentAsByteArray();
            p.setImageData(photo);
        }

        postRepository.saveAll(posts);
    }

}
