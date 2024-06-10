package org.example;

import com.github.britooo.looca.api.core.Looca;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;

public class AreaLoginFuncionarioGeral {

    public static void mostrarMensagemErroMaquina() {
        System.out.println(String.format("""
                ----------------------------------------------------------------
                               ERRO AO BUSCAR DADOS DE MÁQUINA!
                ----------------------------------------------------------------
                Parece que não há nenhum computador vínculado a você. 
                Entre em contato com o suporte de sua empresa para verificar
                o problema.
                """));
        System.exit(0);
    }

    public static void mostrarMensagemErroCredenciais() {
        System.out.println(String.format("""
                ----------------------------------------------------------------
                                     ERRO AO LOGAR!
                ----------------------------------------------------------------
                Login inexistente. Verifique o email e senha novamente ou 
                entre em contato com o suporte técnico da sua empresa.
                """));
        System.exit(0);
    }

    public static void exibirAreaLogadaFuncionarioGeral(Conexao conexao, JdbcTemplate con, BdMySql mysql, BdSqlServer sqlserver, FuncionarioGeral funcionarioLogado, List<Maquina> maquinaFuncionario, Looca looca, boolean verificacaoLogin, String email) {

        System.out.println(String.format( """
                \n
                ----------------------------------------------------------------
                   Olá, %s! O monitoramento de sua máquina irá começar...
                ----------------------------------------------------------------
                * Aperte a tecla ENTER para parar o monitoramento e deslogar.
                """.formatted(funcionarioLogado.getNome())));

        String sql = "SELECT tempoColetaMilissegundos FROM funcionario WHERE email = ?";
        Integer tempoColetaMilissegundos = con.queryForObject(sql, new Object[]{email}, Integer.class);

        long period = tempoColetaMilissegundos; // 10 segundos
        long delay = tempoColetaMilissegundos; // 10 segundos
        if (tempoColetaMilissegundos == null) {
            // Perguntar ao usuário de quanto em quanto tempo deseja fazer o monitoramento
            Scanner scanner = new Scanner(System.in);
            System.out.println("""
            ----------------------------------------------------------------
            Escolha o intervalo de tempo para o monitoramento:
            1. de 5 em 5 segundos
            2. de 10 em 10 segundos
            3. de 15 em 15 segundos
            4. de 20 em 20 segundos
            5. de 25 em 25 segundos
            6. de 30 em 30 segundos(default)
            """);
            int escolha = scanner.nextInt();

            switch (escolha) {
                case 1 -> period = 5000;
                case 2 -> period = 10000;
                case 3 -> period = 15000;
                case 4 -> period = 20000;
                case 5 -> period = 25000;
                case 6 -> period = 30000;
                default -> {
                    System.out.println("Opção inválida! Usando o intervalo padrão de 30 segundos.");
                    period = 30000;
                }
            }

            String updateTime = "UPDATE funcionario SET tempoColetaMilissegundos = " + period + " WHERE email = '" + email + "';";
            con.update(updateTime);
        }

        sqlserver.atualizarStatusLogin(funcionarioLogado);

        Integer idMaquina = maquinaFuncionario.get(0).getIdMaquina();
        String modelo = maquinaFuncionario.get(0).getModelo();
        String fabricante = looca.getSistema().getFabricante();
        String nomeMaquina = maquinaFuncionario.get(0).getNomeMaquina();
        String sistemaOperacional = looca.getSistema().getSistemaOperacional();
        Integer fkEmpresa = maquinaFuncionario.get(0).getFkEmpresa();
        String hostname = looca.getRede().getParametros().getHostName();

        Maquina maquina = new Maquina(idMaquina, modelo, fabricante, nomeMaquina, sistemaOperacional, funcionarioLogado, fkEmpresa);

        if (maquinaFuncionario.get(0).getSistemaOperacional() == null || maquinaFuncionario.get(0).getFabricanteSO() == null) {
            sqlserver.atualizarDadosDaMaquina(maquina);
        }

        Integer conversorGb = 1000000000;

        Double memoriaTotal = looca.getMemoria().getTotal().doubleValue()/conversorGb;
        Double tamanhoTotal = looca.getGrupoDeDiscos().getVolumes().get(0).getTotal().doubleValue()/conversorGb;
        Double frequenciaProcessador = looca.getProcessador().getFrequencia().doubleValue()/conversorGb;

        List<EspecificacaoComponente> especificacoes = sqlserver.buscarListaDeEspecificacoesPorMaquina(maquina);
        sqlserver.atualizarInformacaoEspecificacaoComponente(especificacoes, memoriaTotal, tamanhoTotal, frequenciaProcessador, maquina);

        List<MetricaComponente> metricas = sqlserver.buscarListaDeMetricas(maquina);

        System.out.println(String.format("""
                DADOS INICIAIS

                Id da máquina: %d
                Sistema operacional: %s

                Tamanho do disco: %.2f GB
                Memória total: %.2f GB
                Frequência da CPU: %.2f GHz
                identificador da CPU: %s
                """.formatted(maquina.getIdMaquina(), maquina.getSistemaOperacional(), tamanhoTotal, memoriaTotal, frequenciaProcessador, looca.getProcessador().getIdentificador())));

        //coleta de registros a cada 30 segundos
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {

            public void run() {
                if (verificacaoLogin) {

                    LocalDateTime dataHoraRegistro = LocalDateTime.now();
                    Integer fkMaquina = maquina.getIdMaquina();
                    Integer fkFuncionario = funcionarioLogado.getIdFuncionario();
                    Integer fkEmpresa = maquina.getFkEmpresa();

                    //disco
                    Double espacoDisponivel = looca.getGrupoDeDiscos().getVolumes().get(0).getDisponivel().doubleValue() / conversorGb;
                    String tipoRegistroDisco = "Espaço disponível";
                    Integer fkEspecificacaoComponenteDisco = especificacoes.get(0).getIdEspecificacaoComponente();
                    Integer fkComponenteDisco = especificacoes.get(0).getFkComponente();

                    String statusRegistroEspacoLivre = "";
                    Double porcentagemEspacoLivre = (double) Math.round((espacoDisponivel / tamanhoTotal) * 100);

                    if (porcentagemEspacoLivre <= metricas.get(0).getPorcentagemCritico()) {
                        statusRegistroEspacoLivre = "Crítico";
                        enviarMensagemSlack("Alerta crítico 🚨 : " + hostname + " com espaço em disco abaixo de " + metricas.get(0).getPorcentagemCritico() + "%.");
                    } else if (porcentagemEspacoLivre <= metricas.get(0).getPorcentagemAlerta()) {
                        statusRegistroEspacoLivre = "Alerta";
                        enviarMensagemSlack("Alerta ❗ : " + hostname + " com espaço em disco abaixo de " + metricas.get(0).getPorcentagemAlerta() + "%.");
                    } else {
                        statusRegistroEspacoLivre = "Ideal";
                    }

                    // memória
                    Double memoriaEmUso = looca.getMemoria().getEmUso().doubleValue() / conversorGb;
                    String tipoRegistroMemoria = "Memória em uso";
                    Integer fkEspecificacaoComponenteMemoria = especificacoes.get(1).getIdEspecificacaoComponente();
                    Integer fkComponenteMemoria = especificacoes.get(1).getFkComponente();

                    String statusRegistroMemoriaUso = "";
                    Double porcentagemUsoMemoria = (double) Math.round((memoriaEmUso / memoriaTotal) * 100);

                    if (porcentagemUsoMemoria >= metricas.get(1).getPorcentagemCritico()) {
                        statusRegistroMemoriaUso = "Crítico";
                        enviarMensagemSlack("Alerta crítico 🚨 : " + hostname + " com uso de memória acima de " + metricas.get(1).getPorcentagemCritico() + "%.");
                    } else if (porcentagemUsoMemoria >= metricas.get(1).getPorcentagemAlerta()) {
                        statusRegistroMemoriaUso = "Alerta";
                        enviarMensagemSlack("Alerta ❗ : " + hostname + " com uso de memória acima de " + metricas.get(1).getPorcentagemAlerta() + "%.");
                    } else {
                        statusRegistroMemoriaUso = "Ideal";
                    }

                    //processador
                    Double usoProcessador = looca.getProcessador().getUso().doubleValue();
                    Integer totalProcessos = looca.getGrupoDeProcessos().getTotalProcessos();
                    Double temperaturaCpu = looca.getTemperatura().getTemperatura();
                    String tipoRegistroUsoProcessador = "Uso do processador";
                    String tipoRegistroQtdeProcessos = "Total de processos";
                    String tipoRegistroTemperatura = "Temperatura da CPU";
                    Integer fkEspecificacaoComponenteProcessador = especificacoes.get(2).getIdEspecificacaoComponente();
                    Integer fkComponenteProcessador = especificacoes.get(2).getFkComponente();

                    String statusRegistroUsoProcessador = "";
                    String statusRegistroTemperaturaCpu = "";

                    if (usoProcessador >= metricas.get(2).getPorcentagemCritico()) {
                        statusRegistroUsoProcessador = "Crítico";
                        enviarMensagemSlack("Alerta crítico 🚨 : " + hostname + " com uso de processador acima de " + metricas.get(2).getPorcentagemCritico() + "%.");
                    } else if (usoProcessador >= metricas.get(2).getPorcentagemAlerta()) {
                        statusRegistroUsoProcessador = "Alerta";
                        enviarMensagemSlack("Alerta ❗ : " + hostname + " com uso de processador acima de " + metricas.get(2).getPorcentagemAlerta() + "%.");
                    } else {
                        statusRegistroUsoProcessador = "Ideal";
                    }

                    if (temperaturaCpu >= metricas.get(3).getPorcentagemCritico()) {
                        statusRegistroTemperaturaCpu = "Crítico";
                        enviarMensagemSlack("Alerta crítico 🚨 : " + hostname + " com temperatura da CPU acima de " + metricas.get(3).getPorcentagemCritico() + "°C.");
                    } else if (temperaturaCpu >= metricas.get(3).getPorcentagemAlerta()) {
                        statusRegistroTemperaturaCpu = "Alerta";
                        enviarMensagemSlack("Alerta ❗ : " + hostname + " com temperatura da CPU acima de " + metricas.get(3).getPorcentagemAlerta() + "°C.");
                    } else {
                        statusRegistroTemperaturaCpu = "Ideal";
                    }


                    //inserts
                    sqlserver.registrarEspacoDisponivelEmDisco(dataHoraRegistro, espacoDisponivel, tipoRegistroDisco, fkEspecificacaoComponenteDisco, fkComponenteDisco, fkMaquina, fkFuncionario, fkEmpresa, statusRegistroEspacoLivre);
                    sqlserver.registrarMemoriaEmUso(dataHoraRegistro, memoriaEmUso, tipoRegistroMemoria, fkEspecificacaoComponenteMemoria, fkComponenteMemoria, fkMaquina, fkFuncionario, fkEmpresa, statusRegistroMemoriaUso);
                    sqlserver.registrarUsoProcessador(dataHoraRegistro, usoProcessador, tipoRegistroUsoProcessador, fkEspecificacaoComponenteProcessador, fkComponenteProcessador, fkMaquina, fkFuncionario, fkEmpresa, statusRegistroUsoProcessador);
                    sqlserver.registrarTotalProcessos(dataHoraRegistro, (double) totalProcessos, tipoRegistroQtdeProcessos, fkEspecificacaoComponenteProcessador, fkComponenteProcessador, fkMaquina, fkFuncionario, fkEmpresa);
                    sqlserver.registrarTemperaturaCpu(dataHoraRegistro, temperaturaCpu, tipoRegistroTemperatura, fkEspecificacaoComponenteProcessador, fkComponenteProcessador, fkMaquina, fkFuncionario, fkEmpresa, statusRegistroTemperaturaCpu);

                    mysql.registrarEspacoDisponivelEmDisco(dataHoraRegistro, espacoDisponivel, tipoRegistroDisco, fkEspecificacaoComponenteDisco, fkComponenteDisco, fkMaquina, fkFuncionario, fkEmpresa);
                    mysql.registrarMemoriaEmUso(dataHoraRegistro, memoriaEmUso, tipoRegistroMemoria, fkEspecificacaoComponenteMemoria, fkComponenteMemoria, fkMaquina, fkFuncionario, fkEmpresa);
                    mysql.registrarUsoProcessador(dataHoraRegistro, usoProcessador, tipoRegistroUsoProcessador, fkEspecificacaoComponenteProcessador, fkComponenteProcessador, fkMaquina, fkFuncionario, fkEmpresa);
                    mysql.registrarTotalProcessos(dataHoraRegistro, (double) totalProcessos, tipoRegistroQtdeProcessos, fkEspecificacaoComponenteProcessador, fkComponenteProcessador, fkMaquina, fkFuncionario, fkEmpresa);
                    mysql.registrarTemperaturaCpu(dataHoraRegistro, temperaturaCpu, tipoRegistroTemperatura, fkEspecificacaoComponenteProcessador, fkComponenteProcessador, fkMaquina, fkFuncionario, fkEmpresa);

                    System.out.println("Captura realizada.");

                    try {
                        if (System.in.available() > 0) {
                            System.out.println("""
                                    ----------------------------------------------------------------
                                                       ENCERRANDO MONITORAMENTO...
                                    ----------------------------------------------------------------
                                    """);
                            sqlserver.deslogar(funcionarioLogado);
                            timer.cancel();
                            System.exit(0);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };


        timer.scheduleAtFixedRate(task, delay, period);
    }

    private static void enviarMensagemSlack(String mensagem) {
        JSONObject json = new JSONObject();
        json.put("text", mensagem);
        try {
            Slack.enviarMensagem(json);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
