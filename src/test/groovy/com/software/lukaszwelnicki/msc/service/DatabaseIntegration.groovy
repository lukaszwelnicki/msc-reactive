package com.software.lukaszwelnicki.msc.service

import com.software.lukaszwelnicki.msc.database.MeasurementRepository
import com.software.lukaszwelnicki.msc.generators.DataGenerator
import com.software.lukaszwelnicki.msc.generators.DataGeneratorsSet
import com.software.lukaszwelnicki.msc.measurements.MeasurementCollections
import com.software.lukaszwelnicki.msc.measurements.documents.AftBMT
import com.software.lukaszwelnicki.msc.measurements.documents.Measurement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.Flux
import spock.lang.Specification

import java.time.LocalDateTime
import java.util.stream.Collectors

@DataMongoTest
class DatabaseIntegration extends Specification {

    @Autowired
    ReactiveMongoTemplate mongoTemplate

    @Autowired
    MeasurementRepository<? extends Measurement> measurementRepository


    def setup() {
        Set<DataGenerator<? extends Measurement>> dataGenerators = DataGeneratorsSet.INSTANCE.getDataGenerators()
        LocalDateTime start = LocalDateTime.of(2018, 1, 1, 0, 0, 0)
        LocalDateTime end = LocalDateTime.of(2018, 1, 1, 0, 0, 30)
        dataGenerators.forEach { g -> measurementRepository.saveAll(g.generateRecordsInBetweenDates(start, end, 5)).subscribe() }
    }

    def "should return Flux of valid Measurement type"() {
        given:
            List<AftBMT> records = measurementRepository.findWithTailableCursorByClass(AftBMT.class).collect(Collectors.toList()).block()
        expect:
            records.get(0).class == AftBMT.class
    }

    def cleanup() {
        Flux.fromIterable(MeasurementCollections.getMeasurementClasses())
                .flatMap { c -> mongoTemplate.dropCollection(c) }
                .blockFirst()
    }

}
