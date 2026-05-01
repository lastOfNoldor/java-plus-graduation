package ru.practicum.kafka.serializer;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
    private final DecoderFactory decoderFactory;
    private final Schema schema;

    public BaseAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        this.schema = schema;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            if (data != null) {
                DatumReader<T> reader = new SpecificDatumReader<>(schema);
                BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);
                return reader.read(null, decoder);
            }
            return null;
        } catch (Exception e) {
            throw new SerializationException(
                    String.format("Ошибка десериализации Avro-данных в топике '%s'", topic), e);
        }
    }
}
