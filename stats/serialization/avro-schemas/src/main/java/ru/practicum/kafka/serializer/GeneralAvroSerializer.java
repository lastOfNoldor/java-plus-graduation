package ru.practicum.kafka.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {
    private EncoderFactory encoderFactory;
    public GeneralAvroSerializer() {
        this(EncoderFactory.get());
    }

    public GeneralAvroSerializer(EncoderFactory encoderFactory) {
        this.encoderFactory = encoderFactory;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        this.encoderFactory = EncoderFactory.get();
    }

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] result = null;
            BinaryEncoder encoder = encoderFactory.binaryEncoder(out, null);
            if (data != null) {
                DatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(data.getSchema());
                writer.write(data, encoder);
                encoder.flush();
                result = out.toByteArray();
            }
            return result;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
