#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.worker;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Stream;

class Contact {
    final String name;
    final String email;
    final String phone;

    Contact(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    static Stream<Contact> readContacts(File file) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            return reader.readAll().stream()
                         .map(line -> new Contact(line[0], line[1], line[2]));
        }
    }

    static boolean containsContacts(File file) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            return reader.readNext().length == 3;
        }
    }
}
