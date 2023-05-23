package br.com.vulcan.jvulcan.api.entity.post.service;

import br.com.vulcan.jvulcan.api.entity.novel.repository.NovelRepository;
import br.com.vulcan.jvulcan.api.entity.post.model.Post;
import br.com.vulcan.jvulcan.api.entity.novel.model.Novel;

import br.com.vulcan.jvulcan.api.infrastructure.exception.MessageNotSentException;
import br.com.vulcan.jvulcan.api.infrastructure.exception.NovelNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.NoArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@NoArgsConstructor
@PropertySource("classpath:application.properties")
public class PostService implements IPostService
{

    private final String NOVEL_NOT_FOUND = "A novel requisitada não existe na base de dados!";
    private final String MESSAGE_NOT_SENT = "A mensagem não foi enviada.";

    @Value("${webhook_url}")
    private String webhookUrl;

    @Autowired
    NovelRepository novelRepository;

    @PostConstruct
    public void init()
    {
        System.out.println("Conectado no WebHook: ".concat(webhookUrl));
    }

    /**
     * Envia uma embed via Webhook com informações de uma nova postagem no site.
     * @param post O post que será notificado via Webhook.
     */
    @Override
    public void notificarNovaPostagem(Post post) throws NovelNotFoundException, MessageNotSentException {

        try{

            Optional<Novel> optionalNovel = novelRepository.findByNome(post.getCategoria());

            String cargoMarcado = "ROLE_NOT_FOUND";
            String capaUrl = "";
            var autor = "USERNAME_UNDEFINED";

            if(optionalNovel.isPresent())
            {
                Novel novel = optionalNovel.get();

                cargoMarcado = novel.getIdCargo();
                String capa = novel.getCapa();
                String padrao = "-\\\\d+x\\\\d+";
                capaUrl = capa.replaceAll(padrao, "");
                autor = novel.getAutor();
            } else
            {

                throw new NovelNotFoundException(NOVEL_NOT_FOUND);

            }

            String mensagemJson =
            """
                    {
                        "content" : " ⚡ <@&863456249873825812> <@&%s>",
                        "embeds" : [
                                        {
                                            "title" : "%s",
                                            "url" : "%s",
                                            "author" : {
                                                "name" : "%s",
                                                "icon_url" : "%s"
                                            },
                                            "color" : 47615,
                                            "footer" : {
                                                "text" : "Clique no título para ler o capítulo",
                                                "icon_url" : "%s"
                                            }
    
                                        }
                                    ]
                    }
            """.formatted(cargoMarcado, post.getTitulo(), post.getLink(), autor, post.getLinkAvatarAutor(), capaUrl);

            HttpClient cliente  = HttpClient.newHttpClient();
            HttpRequest requisicao = HttpRequest.newBuilder()
                                                .uri(URI.create(this.webhookUrl))
                                                .header("Content-Type", "application/json")
                                                .POST(HttpRequest.BodyPublishers.ofString(mensagemJson, StandardCharsets.UTF_8))
                                                .build();

            HttpResponse<String> response = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                System.out.println("Mensagem enviada com sucesso!");
            } else {
                throw new MessageNotSentException(MESSAGE_NOT_SENT);
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }
}