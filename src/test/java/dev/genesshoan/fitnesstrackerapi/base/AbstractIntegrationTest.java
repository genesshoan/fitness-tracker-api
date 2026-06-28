package dev.genesshoan.fitnesstrackerapi.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEntityFactory.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractIntegrationTest extends AbstractContainerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestEntityFactory testEntityFactory;

    @Autowired
    protected ObjectMapper objectMapper;
}
