package com.prasanna.interview.validation;

import com.prasanna.interview.exception.MalformedJsonException;
import com.prasanna.interview.exception.ValidationException;
import com.prasanna.interview.model.DigitalOrder;
import com.prasanna.interview.model.OrderEvent;
import com.prasanna.interview.model.PhysicalOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Parses direct order-event JSON into the sealed {@link OrderEvent} hierarchy.
 *
 * <p>The parser reads {@code orderType} before binding so unsupported event types are rejected explicitly and the
 * processing service can rely on exhaustive pattern matching over known records.</p>
 */
@Component
public class OrderEventParser {

    private final ObjectMapper objectMapper;

    /**
     * Creates the parser.
     *
     * @param objectMapper JSON mapper configured for strict or lenient binding
     */
    public OrderEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses an order JSON payload into a concrete order record.
     *
     * @param messageJson direct order JSON payload
     * @return parsed {@link DigitalOrder} or {@link PhysicalOrder}
     * @throws ValidationException when {@code orderType} is missing or unsupported
     * @throws MalformedJsonException when JSON parsing fails
     */
    public OrderEvent parse(String messageJson) {
        try {
            JsonNode root = objectMapper.readTree(messageJson);
            JsonNode orderTypeNode = root.get("orderType");
            if (orderTypeNode == null || orderTypeNode.asText().isBlank()) {
                throw new ValidationException("orderType is required");
            }
            String orderType = orderTypeNode.asText();
            return switch (orderType) {
                case "DIGITAL" -> objectMapper.treeToValue(root, DigitalOrder.class);
                case "PHYSICAL" -> objectMapper.treeToValue(root, PhysicalOrder.class);
                default -> throw new ValidationException("Unsupported orderType: " + orderType);
            };
        } catch (ValidationException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new MalformedJsonException("Malformed SNS Message JSON", e);
        }
    }
}
