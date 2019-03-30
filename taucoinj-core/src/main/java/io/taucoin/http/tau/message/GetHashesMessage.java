package io.taucoin.http.tau.message;

import io.taucoin.http.message.Message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class GetHashesMessage extends Message {

    // Starting block number.
    private long startNumber;

    private long max;

    // get block hashes in the reverse order.
    private boolean reverse;

    public GetHashesMessage() {
    }

    public GetHashesMessage(long startNumber, long max, boolean reverse) {
        this.startNumber = startNumber;
        this.max = max;
        this.reverse = reverse;
    }

    public long getStartNumber() {
        return startNumber;
    }

    public void setStartNumber(long startNumber) {
        this.startNumber = startNumber;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public boolean getReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    @Override
    public Class<HashesMessage> getAnswerMessage() {
        return HashesMessage.class;
    }

    @Override
    public String toJsonString() {
        return MessageFactory.createJsonString(this);
    }

    @Override
    public String toString() {
        StringBuilder payload = new StringBuilder();
        payload.append("\nGetHashesMessage[\n");
        payload.append("\tstartNumber:" + startNumber + ",\n");
        payload.append("\tmax:" + max + ",\n");
        payload.append("\treverse:" + reverse + "\n");
        payload.append("]\n");
        return payload.toString();
    }

    public static class Serializer extends StdSerializer<GetHashesMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<GetHashesMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                GetHashesMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField("start", message.getStartNumber());
                jsonGenerator.writeNumberField("max", message.getMax());
                jsonGenerator.writeBooleanField("reverse", message.getReverse());
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<GetHashesMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public GetHashesMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            GetHashesMessage message = new GetHashesMessage();

            ObjectCodec codec = parser.getCodec();
            JsonNode node = null;
            try {
                node = codec.readTree(parser);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("deserialize message erorr {}", e);
                return null;
            }

            JsonNode startNode = node.get("start");
            message.setStartNumber(startNode.asLong());
            JsonNode maxNode = node.get("max");
            message.setMax(maxNode.asLong());
            JsonNode reverseNode = node.get("reverse");
            message.setReverse(reverseNode.asBoolean());

            return message;
        }
    }
}