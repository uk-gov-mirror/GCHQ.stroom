package stroom.util.logging;


import stroom.util.logging.AsciiTable.Field;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

class TestAsciiTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestAsciiTable.class);

    @Test
    void test() {

        List<Pojo> sourceData = List.of(
               new Pojo("Mr", "Joe", "Bloggs",
                       LocalDate.of(1971, 3, 23), 180),
                new Pojo("Mrs", "Joanna", "Bloggs",
                        LocalDate.of(1972, 4, 1), 170),
                new Pojo("Mr", "No Surname", null,
                        LocalDate.of(1972, 4, 1), 170)

        );

        final String table = AsciiTable.from(sourceData)
                .withField(Field.of("Title", Pojo::getTitle))
                .withField(Field.of("First Name", Pojo::getFirstName))
                .withField(Field.builder("Surname", Pojo::getSurname)
                        .withNullValueSupplier(() -> "-")
                        .build())
                .withField(Field.builder("Date of Birth", Pojo::getDob)
                        .centerAligned()
                        .build())
                .withField(Field.builder("Height", Pojo::getHeightCm)
                        .rightAligned()
                        .withFormat(val -> val + "cm")
                        .build())
//                .withRowLimt(1)
                .build();

        LOGGER.info("table:\n{}", table);
    }

    private static class Pojo {
        private final String title;
        private final String firstName;
        private final String surname;
        private final LocalDate dob;
        private final Integer heightCm;

        public Pojo(final String title,
                    final String firstName,
                    final String surname,
                    final LocalDate dob,
                    final Integer heightCm) {
            this.title = title;
            this.firstName = firstName;
            this.surname = surname;
            this.dob = dob;
            this.heightCm = heightCm;
        }

        public String getTitle() {
            return title;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getSurname() {
            return surname;
        }

        public LocalDate getDob() {
            return dob;
        }

        public Integer getHeightCm() {
            return heightCm;
        }
    }
}