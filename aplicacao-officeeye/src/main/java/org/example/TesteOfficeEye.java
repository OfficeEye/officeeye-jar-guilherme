package org.example;

import com.github.britooo.looca.api.group.memoria.Memoria;
import java.util.List;
import java.util.Scanner;
import com.github.britooo.looca.api.core.Looca;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import static oshi.util.FormatUtil.formatBytes;

public class TesteOfficeEye {
    private static final Logger log = LoggerFactory.getLogger(TesteOfficeEye.class);

    private static final String LOG_DIR = "logs/";
    private static final String LOG_FILE_PREFIX = "Log-OfficeEye-";
    private static final long MAX_LOG_SIZE = 20 * 1024; // 20KB
    private static File logFileAtual;

    public static void main(String[] args) {
        Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

        try {
            // Scanner para login
            Scanner leitorLogin = new Scanner(System.in);
            // Temporizador para o laço de repetição
            Timer timer = new Timer();

            // Conexão com o banco de dados
            Conexao conexao = new Conexao();
            JdbcTemplate con = conexao.getConexaoDoBanco();

            // Instância do looca para coletar dados
            Looca looca = new Looca();

            boolean continuarTentando = true;

            while (continuarTentando) {
                // Login
                System.out.println("""
                        ----------------------------------------------------------------
                                    Bem-Vindo ao monitoramento officeEye!
                        ----------------------------------------------------------------
                                                    LOGIN
                        Email:
                        """);

                String email = leitorLogin.nextLine();

                if (!emailPattern.matcher(email).matches()) {
                    System.out.println("Formato de email inválido. Tente novamente.");
                    System.exit(0);
                }

                System.out.println("Senha:");
                String senha = leitorLogin.nextLine();

                // Instância do login de funcionário para que haja a verificação dos dados
                LoginFuncionario login = new LoginFuncionario();

                Boolean verificacaoLogin = login.verificarLogin(email, senha, con);

                if (verificacaoLogin) {
                    List<Maquina> maquinaFuncionario = con.query((String.format("SELECT * FROM maquina WHERE fkFuncionario = '%d' and fkEmpresa = '%d'", login.getIdFuncionario(), login.getFkEmpresa())),
                            new BeanPropertyRowMapper<>(Maquina.class));

                    if (maquinaFuncionario.isEmpty()) {
                        System.out.println("""
                                \n
                                --------------------------------------------------------------------------------------
                                      Não há nenhuma máquina vinculada a você! Entre em contato com sua empresa.
                                --------------------------------------------------------------------------------------
                                """);

                        System.exit(0);

                    } else {

                        System.out.printf("""                        
                                ----------------------------------------------------------------
                                   Olá, %s! O monitoramento de sua máquina irá começar em 5s
                                ----------------------------------------------------------------
                                %n""", login.getNome());

                        Integer idMaquina = maquinaFuncionario.get(0).getIdMaquina();
                        String modelo = maquinaFuncionario.get(0).getModelo();
                        String fabricante = looca.getSistema().getFabricante();
                        String nomeMaquina = maquinaFuncionario.get(0).getNomeMaquina();
                        String sistemaOperacional = looca.getSistema().getSistemaOperacional();
                        Integer fkFuncionario = login.getIdFuncionario();
                        Integer fkEmpresa = login.getFkEmpresa();

                        Maquina maquina = new Maquina(idMaquina, modelo, fabricante, nomeMaquina, sistemaOperacional, fkFuncionario, fkEmpresa);

                        if (maquinaFuncionario.get(0).getSistemaOperacional() == null) {

                            con.update("UPDATE maquina SET fabricante = ?,"
                                            + "sistemaOperacional  = ? WHERE idmaquina= ?",
                                    maquina.getFabricante(), maquina.getSistemaOperacional(), maquina.getIdMaquina());
                        }

                        int conversor = 1000000000;
                        Double memoriaTotal = looca.getMemoria().getTotal().doubleValue() / conversor;
                        Double tamanhoTotal = looca.getGrupoDeDiscos().getVolumes().get(0).getTotal().doubleValue() / conversor;
                        Double frequenciaProcessador = looca.getProcessador().getFrequencia().doubleValue() / conversor;


                        con.update("UPDATE especificacaoComponentes SET informacaoTotalEspecificacao = ?"
                                        + "WHERE fkMaquina= ? and idEspecificacaoComponentes = 1",
                                tamanhoTotal, maquina.getIdMaquina());

                        con.update("UPDATE especificacaoComponentes SET informacaoTotalEspecificacao = ?"
                                        + "WHERE fkMaquina= ? and idEspecificacaoComponentes = 2 ",
                                memoriaTotal, maquina.getIdMaquina());

                        con.update("UPDATE especificacaoComponentes SET informacaoTotalEspecificacao = ?"
                                        + "WHERE fkMaquina= ? and idEspecificacaoComponentes = 3 ",
                                frequenciaProcessador, maquina.getIdMaquina());

                        EspecificacaoComponente especificacaoComponente = new EspecificacaoComponente();
                        List<EspecificacaoComponente> especificacoesDaMaquina = especificacaoComponente.buscarListaDeEspecificacoesPorMaquina(maquina.getIdMaquina(), con);

                        TimerTask task = new TimerTask() {
                            static int contador = 0;

                            public void run() {
                                // MONITORAMENTO DA MEMÓRIA RAM - LIMPEZA AUTOMÁTICA DA MEMÓRIA DO INTELLIJ

                                con.update("INSERT INTO registrosEspecificacaoComponente (dataHoraRegistro, registroNumero, tipoRegistro, fkEspecificacaoComponentes, fkComponente, fkMaquina, fkFuncionario, fkEmpresa) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                        LocalDateTime.now(), looca.getMemoria().getEmUso().doubleValue() / conversor, "Memória em uso", especificacoesDaMaquina.get(1).getIdEspecificacaoComponentes(), 2, maquina.getIdMaquina(), login.getIdFuncionario(), maquina.getFkEmpresa());

                                System.out.println("\n\n\n\n\nCaptura realizada.");
                                Memoria memoria = looca.getMemoria();

                                // Monitoramento em Gib (GibiByte)
                                // 1 Gib = 1.07 GB (GigaByte)
                                System.out.println(memoria);

                                Runtime runtime = Runtime.getRuntime();

                                if (looca.getMemoria().getEmUso().doubleValue() / conversor > 7.00) {
                                    contador++;
                                    System.out.println("Alto uso detectado! Limpando a memória...");
                                    limpezaDaMemoria();
                                    System.out.println("Limpeza concluída.");

                                    System.out.println
                                            // 10 elevado a 6 = 1 MegaByte (MB)
                                                    ("Uso de memória pela JVM : " + runtime.totalMemory() / Math.pow(10, 6) + "MB");

                                    if (contador >= 5) {
                                        // Aviso pro usuário
                                        System.out.println("""


                                                A memória do seu IntelliJ já foi limpa 5 vezes seguidas, porém sua memória continua cheia! Feche alguns aplicativos!!""");
                                        System.exit(0);
                                    }

                                } else {
                                    // contador reseta se a memória não for limpa 5 vezes SEGUIDAS
                                    contador = 0;
                                }
                            }

                            public static void limpezaDaMemoria() {
                                Runtime runtime = Runtime.getRuntime();
                                long memoriaAntes = runtime.totalMemory() - runtime.freeMemory();
                                System.gc();
                                long memoriaDepois = runtime.totalMemory() - runtime.freeMemory();
                                long memoriaLiberada = memoriaAntes - memoriaDepois;
                                System.out.println("Memória limpa com sucesso!");
                                System.out.println("Memória liberada: " + formatBytes(memoriaLiberada));
                            }
                        };

                        // 5 segundos como demonstração. O ideal é de 5 em 5 minutos (300 segundos ou delay = 300000)
                        long delay = 5000; // 5 segundos
                        long period = 5000; // 5 segundos

                        timer.scheduleAtFixedRate(task, delay, period);
                    }

                    continuarTentando = false; // Sair do loop se o login for bem-sucedido
                } else {
                    // Registrar falha de login
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");
                    String dataHoraAtual = LocalDateTime.now().format(formatter);

                    if (logFileAtual == null || logFileAtual.length() >= MAX_LOG_SIZE) {
                        logFileAtual = new File(LOG_DIR + LOG_FILE_PREFIX + dataHoraAtual + ".txt");
                    }

                    try (FileWriter fw = new FileWriter(logFileAtual, true)) {
                        String mensagemErro = String.format(
                                "Erro ao logar! Esse login não existe. Verifique o e-mail e senha novamente ou entre em contato com o suporte técnico da sua empresa. Data: %s, Email de acesso: %s%n",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")),
                                email
                        );
                        fw.write(mensagemErro);
                        System.out.println("""
                                \n
                                --------------------------------------------------------------------------------------------------------------------
                                Erro ao logar!
                                --------------------------------------------------------------------------------------------------------------------
                                Esse login não existe. Verifique o e-mail e senha novamente ou entre em contato com o suporte técnico da sua empresa.
                                """);

                    } catch (IOException e) {
                        log.error("Erro ao criar o arquivo de log: ", e);
                        e.printStackTrace();
                    }

                    // Perguntar ao usuário se deseja tentar novamente
                    System.out.println("""
                            Deseja tentar outro login?
                            1- Sim
                            Qualquer outro dígito - Não/Sair
                            """);

                    int resposta = leitorLogin.nextInt();
                    leitorLogin.nextLine(); // Consumir nova linha

                    if (resposta != 1) {
                        continuarTentando = false;
                        System.exit(0);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Erro durante a execução do programa: ", e);
            e.printStackTrace();
        }
    }
}
