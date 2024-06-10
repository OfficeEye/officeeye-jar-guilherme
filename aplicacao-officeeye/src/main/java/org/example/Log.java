package org.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class Log {
    public static void generateLog(String message, String typeLog, Exception e) throws IOException {

        // definindo o diretório que vai ser criado
        final Path path = Paths.get("Logs");

        // modelo de formatação de data
        final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // data e hora atual
        LocalDate date = LocalDate.now();
        LocalDateTime dateTimeNow = LocalDateTime.now();

        // formatação da data e hora atual com o modelo
        String dateFormatted = df.format(date);
        String dateTimeFormatted = dtf.format(dateTimeNow);

        // nome do arquivo que será gerado
        String file = "%s-OfficeEye-log.log".formatted(dateFormatted);

        // verifica se o diretório já existe
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }

        // gravando o arquivo dentro do diretório
        File log = new File("%s/%s".formatted(path, file));

        // se não existir nenhum arquivo com o mesmo nome, um novo arquivo é criado
        // gravando apenas data, pois será um arquivo de log por dia
        if (!log.exists()) {
            log.createNewFile();
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callingClass = stackTrace[2].getClassName();
        int callingLine = stackTrace[2].getLineNumber();

        // Cria o gravador de arquivo
        FileWriter fw = new FileWriter(log, true);
        BufferedWriter bw = new BufferedWriter(fw);


        bw.write("""
                [ %s %s  %s:%d - %s : %s]
                """.formatted(dateTimeFormatted, typeLog, callingClass, callingLine, message, e));
        bw.newLine();

        bw.close();
        fw.close();
    }

    public static void generateLog(String message, String typeLog) throws IOException {

        // definindo o diretório que vai ser criado
        final Path path = Paths.get("Logs");

        // modelo de formatação de data
        final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // data e hora atual
        LocalDate date = LocalDate.now();
        LocalDateTime dateTimeNow = LocalDateTime.now();

        // formatação da data e hora atual
        String dateFormatted = df.format(date);
        String dateTimeFormatted = dtf.format(dateTimeNow);

        // nome do arquivo que será gerado
        String file = "OfficeEye-log-%s.log".formatted(dateFormatted);

        // verifica se o diretório já existe
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }

        // gravando o arquivo dentro do diretório
        File log = new File("%s/%s".formatted(path, file));

        // se não existir nenhum arquivo com o mesmo nome, um novo arquivo é criado
        // como estou gravando apenas data, pois será um arquivo de log por dia, apenas 1 arquivo é gerado por dia
        if (!log.exists()) {
            log.createNewFile();
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callingClass = stackTrace[2].getClassName();
        int callingLine = stackTrace[2].getLineNumber();

        // Cria o gravador de arquivo
        FileWriter fw = new FileWriter(log, true);
        BufferedWriter bw = new BufferedWriter(fw);


        bw.write("""
                [ %s %s  %s:%d - %s ]
                """.formatted(dateTimeFormatted, typeLog, callingClass, callingLine, message));
        bw.newLine();

        bw.close();
        fw.close();

    }
}