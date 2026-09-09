package cl.duoc.vidasalud.bff;

import cl.duoc.vidasalud.bff.controller.UserController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class MsVidasaludBffApplicationTests {

    @Autowired
    private UserController userController;

    @Test
    @DisplayName("Verifica la carga del contexto Spring Boot y los Beans principales")
    void contextLoads() {
        assertThat(userController).isNotNull();
    }
}
