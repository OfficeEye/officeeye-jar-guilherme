package org.example;

import com.github.britooo.looca.api.core.Looca;
import com.github.britooo.looca.api.group.discos.DiscoGrupo;
import com.github.britooo.looca.api.group.memoria.Memoria;
import com.github.britooo.looca.api.group.processador.Processador;
import com.github.britooo.looca.api.group.processos.ProcessoGrupo;
import com.github.britooo.looca.api.group.servicos.ServicoGrupo;
import com.github.britooo.looca.api.group.sistema.Sistema;
import com.github.britooo.looca.api.group.temperatura.Temperatura;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.List;

public class TesteOfficeEye {

    public static void main(String[] args) {

        //scanner para login
        Scanner leitorLogin = new Scanner(System.in);
        Scanner leitorMenu = new Scanner(System.in);

        //temporizador para o laço de repetição
        Timer timer = new Timer();

        //instâncias dos bancos de dados
        BdMySql mysql = new BdMySql();
        BdSqlServer sqlserver = new BdSqlServer();

        //instância da conexão com o banco local
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        //instância do looca para coletar dados
        Looca looca = new Looca();

        //escolha do tipo de login
        System.out.println(String.format("""
                ----------------------------------------------------------------
                            Bem-Vindo ao monitoramento officeEye!
                ----------------------------------------------------------------
                       Escolha abaixo o número do tipo de login:
                             
                1- Funcionário
                2- Administrador
                """));

        Integer tipoLogin = leitorMenu.nextInt();

        if (tipoLogin.equals(1) && tipoLogin != null) {

            // Login
            System.out.println(String.format("""
                    ----------------------------------------------------------------
                                           LOGIN - FUNCIONÁRIO
                    ----------------------------------------------------------------
                                               
                    Email:
                    """));
            String email = leitorLogin.nextLine();
            System.out.println("Senha:");
            String senha = leitorLogin.nextLine();


            Boolean verificacaoLogin = sqlserver.verificarLoginFuncionario(email, senha);

            if (verificacaoLogin) {

                List<Maquina> maquinaFuncionario = sqlserver.buscarMaquinaDoFuncionario();
                FuncionarioGeral funcionarioLogado = sqlserver.buscarFuncionarioLogado();

                if (maquinaFuncionario.isEmpty()) {
                    AreaLoginFuncionarioGeral.mostrarMensagemErroMaquina();
                } else {
                    AreaLoginFuncionarioGeral.exibirAreaLogadaFuncionarioGeral(conexao, con, mysql, sqlserver, funcionarioLogado, maquinaFuncionario, looca, verificacaoLogin, email);
                }
            } else {
                AreaLoginFuncionarioGeral.mostrarMensagemErroCredenciais();
            }

        } else {
            // Login
            System.out.println(String.format("""
                    ----------------------------------------------------------------
                                           LOGIN - ADMINISTRADOR
                    ----------------------------------------------------------------
                                               
                    Email:
                    """));
            String email = leitorLogin.nextLine();
            System.out.println("Senha:");
            String senha = leitorLogin.nextLine();

            Boolean verificacaoLoginAdm = sqlserver.verificarLoginAdministrador(email, senha);

            if (verificacaoLoginAdm) {
                AreaLoginFuncionarioAdm.exibirAreaLogadaFuncionarioAdm(sqlserver);
            } else {
                AreaLoginFuncionarioAdm.mostrarMensagemErroCredenciais();
            }
        }
    }
}
