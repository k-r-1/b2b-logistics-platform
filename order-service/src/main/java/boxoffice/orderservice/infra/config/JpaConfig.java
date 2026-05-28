package boxoffice.orderservice.infra.config;

import com.boxoffice.common.config.AuditorAwareImpl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaConfig {

  @PersistenceContext
  private EntityManager entityManager;

  @Bean
  public AuditorAwareImpl auditorAwareImpl() {
    return new AuditorAwareImpl();
  }

  @Bean
  public JPAQueryFactory jpaQueryFactory() {
    return new JPAQueryFactory(entityManager);
  }
}
