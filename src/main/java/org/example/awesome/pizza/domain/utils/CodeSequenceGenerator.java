package org.example.awesome.pizza.domain.utils;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.FlushMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.query.NativeQuery;

import java.util.EnumSet;

@Slf4j
public class CodeSequenceGenerator implements BeforeExecutionGenerator {

  @Override
  public Object generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o, Object o1, EventType eventType) {
    final SessionFactoryImplementor sessionFactory = sharedSessionContractImplementor.getSessionFactory();
    final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
    final Dialect dialect = jdbcServices.getDialect();

    final String nextValStatement = dialect.getSequenceSupport().getSequenceNextValString("AWESOMESCHEMA.order_code");

    final Object result;
    try (final SessionImplementor session = sessionFactory.openSession()) {
      final NativeQuery<Object> query = session.createNativeQuery(nextValStatement, Object.class);
      query.setHibernateFlushMode(FlushMode.COMMIT);

      result = query.getSingleResult();
    }

    return result;
  }

  @Override
  public EnumSet<EventType> getEventTypes() {
    return EnumSet.of(EventType.INSERT);
  }
}
