package com.prasanna.interview.validation;

import com.prasanna.interview.TestFixtures;
import com.prasanna.interview.exception.MalformedJsonException;
import com.prasanna.interview.exception.ValidationException;
import com.prasanna.interview.model.DigitalOrder;
import com.prasanna.interview.model.OrderEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderEventParserTest {

    @Test
    void parsesDigitalOrderRecord() {
        OrderEventParser parser = new OrderEventParser(TestFixtures.objectMapper(true));

        OrderEvent event = parser.parse(TestFixtures.digitalOrderJson("evt-parser"));

        assertThat(event).isInstanceOf(DigitalOrder.class);
        assertThat(event.eventId()).isEqualTo("evt-parser");
    }

    @Test
    void rejectsUnknownOrderType() {
        OrderEventParser parser = new OrderEventParser(TestFixtures.objectMapper(true));

        assertThatThrownBy(() -> parser.parse("{\"orderType\":\"OTHER\"}"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unsupported orderType");
    }

    @Test
    void rejectsMalformedJson() {
        OrderEventParser parser = new OrderEventParser(TestFixtures.objectMapper(true));

        assertThatThrownBy(() -> parser.parse("{bad-json"))
                .isInstanceOf(MalformedJsonException.class);
    }
}
