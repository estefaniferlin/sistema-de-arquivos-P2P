package peer;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import java.io.*;
import java.util.HashMap;
import org.jgroups.Address;
import org.jgroups.View;
import util.Mensagem;
import util.Status;

public class Peer extends ReceiverAdapter {

    private JChannel channel;
    private View view;

    private HashMap<String, String> arqLocais;
    private HashMap<String, Address> arqGlobais;

    private void start() throws Exception {
    
        channel = new JChannel().setReceiver(this);
        channel.connect("Arquivos");
        
        arqLocais = new HashMap<>();
        arqGlobais = new HashMap<>();
        
        sendInitializationMessage();
        
        eventLoop(); 
        
        channel.close();
    }
    
    private void sendInitializationMessage() throws Exception {
        Mensagem mensagem = new Mensagem("NOVO_PEER");
        channel.send(null, mensagem);
    }

    @Override
    public void viewAccepted(View novaView) {
        this.view = novaView;
    }
    
    public static void main(String[] args) throws Exception {
        new Peer().start();
    }

    private void eventLoop() {

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Mensagem mensagem;
        String nomeArq, conteudoArq, msgUser;
        int opcao;
        Boolean continuar = true;

        try {
            while (continuar) {
                System.out.println("\n----------< TORRENT DE ARQUIVOS P2P >----------\n");
                System.out.println("1 - Adicionar arquivo");
                System.out.println("2 - Remover arquivo");
                System.out.println("3 - Mostrar lista de arquivos locais");
                System.out.println("4 - Mostrar lista de arquivos globais");    
                System.out.println("5 - Visualizar conteúdo do arquivo");
                System.out.println("6 - Mostrar os peers");
                System.out.println("0 - Sair");

                System.out.println("\nOpção: ");
                opcao = Integer.parseInt(in.readLine());

                switch (opcao) {  
                    case 1: // Adicionar arquivo
                        System.out.println("\n> Informe o nome para o arquivo: ");
                        nomeArq = in.readLine().toLowerCase();

                        System.out.println("\n> Informe o conteúdo para o arquivo: ");
                        conteudoArq = in.readLine().toLowerCase();
                        
                        if (arqGlobais.get(nomeArq) == null) { // Verifica se o arquivo não existe mesmo
                            arqLocais.put(nomeArq, conteudoArq);
                            arqGlobais.put(nomeArq, this.channel.address());
                            
                            mensagem = new Mensagem("ARQUIVO_ADICIONADO");
                            mensagem.setParam("nome_arquivo", nomeArq);
                            
                            channel.send(null, mensagem);   
                            
                            System.out.println("\nO arquivo " + nomeArq + " foi adicionado com sucesso!");     
                        } else // Se existir não deixa salvar
                            System.out.println("\nO arquivo não pode ser criado pois já existe!");             
                        break;
                    case 2: // Deletar um arquivo                        
                        System.out.print("\n> Informe o nome do arquivo que deseja deletar: ");
                        nomeArq = in.readLine().toLowerCase();
                        
                        if(arqLocais.get(nomeArq) != null){ // se o arquivo existir   
                            arqLocais.remove(nomeArq);
                            arqGlobais.remove(nomeArq);
                            
                            mensagem = new Mensagem("ARQUIVO_DELETADO");
                            mensagem.setParam("nome_arquivo", nomeArq);
                            
                            channel.send(null, mensagem);                         
                            System.out.println("\nO arquivo " + nomeArq + " foi deletado com sucesso!");     
                        }else
                            System.out.println("\n** O arquivo não foi deletado pois não existe! **");  
                    break;
                    case 3: // Listar arquivos locais do peer      
                        if(!arqLocais.isEmpty()){
                            System.out.println("\nArquivos locais: ");
                            System.out.println(listaArqLocais());
                        }else{
                            System.out.println("\n** Nenhum arquivo para mostrar! **");
                        }
                        break;
                    case 4: // Listar arquivos globais        
                        if(!arqGlobais.isEmpty()){
                            System.out.println("\nArquivos globais:");
                            System.out.println(listaArqGlobais());
                        }else{
                            System.out.println("\n** Nenhum arquivo para mostrar! **");
                        }
                        break;
                    
                    case 5: // Visualizar conteúdo do arquovo
                        System.out.println("\n> Informe o nome do arquivo que deseja visualizar: ");
                        nomeArq = in.readLine().toLowerCase();
 
                        if (arqLocais.get(nomeArq) != null) { // Se o arquivo existe localmente     
                            System.out.println("\nConteúdo: " + arqLocais.get(nomeArq));         
                        } else if(arqGlobais.get(nomeArq) != null) { // Se o arquivo não existe localmente, mas globalmente
                            
                            mensagem = new Mensagem("SOLICITA_CONTEUDO_ARQUIVO");
                            mensagem.setParam("nome_arquivo", nomeArq);
                            
                            channel.send(null, mensagem);                     
                        } else{
                            System.out.println("\n** O arquivo não existe em nenhum Peer! **");
                        }
                    break;
                    case 6: // Mostrar os peers
                        System.out.println("\nPEERS: ");
                        System.out.println(view); 
                    break;
                    default:
                        System.out.println("\nOpção inválida!");
                    break;
                }
                
                Thread.sleep(1000);
                boolean end = false;
                do{
                    System.out.println("\n> Deseja continuar? sim ou nao");
                    msgUser = in.readLine().toLowerCase();

                    if(msgUser.equals("sim")){
                        continuar = true;
                        end = true;
                        limparTela();
                    }else if(msgUser.equals("nao")){
                        continuar = false;
                        end = true;
                    }else{
                        System.out.println("\n** Opção inválida! **");
                        end = false;
                    }
                }while(!end);
            }
        } catch (Exception e) {
            System.out.println("\n** Erro na comunicação com os peers! **");
            System.out.println("ERRO: " + e);
        }
    }
 
    @Override
    public void receive(Message m) {
        Mensagem mensagem = (Mensagem) m.getObject();
        String nomeArq;

        try {
            if(m.getSrc() != this.channel.address()){
                switch(mensagem.getOperacao()){
                    case "NOVO_PEER":
                        mensagem = new Mensagem("RESPOSTA_" + mensagem.getOperacao());
                        mensagem.setStatus(Status.OK);

                        String listaPeers = "";

                        for (String nome : arqGlobais.keySet()) {
                            listaPeers += nome + "=" + arqGlobais.get(nome) + ";";
                        }

                        mensagem.setParam("lista_arquivos", listaPeers);                     
                        channel.send(m.getSrc(), mensagem);  
                    break;
                    case "RESPOSTA_NOVO_PEER":         
                        String listaArquivos[] = mensagem.getParam("lista_arquivos").split(";");                
                        for (String a : listaArquivos) {                 
                            String dadosArquivo[] = a.split("=");
                            arqGlobais.put(dadosArquivo[0], stringToAddress(dadosArquivo[1]));                      
                        }             
                        break;          
                    case "ARQUIVO_ADICIONADO":          
                        nomeArq = mensagem.getParam("nome_arquivo");
                        arqGlobais.put(nomeArq, m.getSrc());          
                    break;   
                    case "ARQUIVO_DELETADO":   
                        nomeArq = mensagem.getParam("nome_arquivo");
                        arqGlobais.remove(nomeArq); 
                    break;  
                    case "SOLICITA_CONTEUDO_ARQUIVO":             
                        nomeArq = mensagem.getParam("nome_arquivo");
                        mensagem = new Mensagem("RESPOSTA_" + mensagem.getOperacao());
                        mensagem.setStatus(Status.OK);
                        mensagem.setParam("conteudo_arquivo", arqLocais.get(nomeArq));
                        channel.send(m.getSrc(), mensagem);  
                    break; 
                    case "RESPOSTA_SOLICITA_CONTEUDO_ARQUIVO":                
                        System.out.println("\nConteúdo: " + mensagem.getParam("conteudo_arquivo"));
                }
            }
        } catch (Exception e) {
            System.out.println("** Erro na comunicação com os peers! **");
            System.out.println("ERRO: " + e);
        }
    }
    
    public String listaArqLocais(){  
        String lista = "";
        
        for(String nome : arqLocais.keySet()){
            lista += "\n------------------------------------------------";
            lista += "\n|Nome: " + nome;
            lista += "\n|Conteúdo: " + arqLocais.get(nome);      
        }  
        return lista;    
    }
    
    public String listaArqGlobais(){ 
        String lista = "";
        
        for(String nome : arqGlobais.keySet()){ 
            lista += "\n------------------------------------------------";
            lista += "\n|Nome: " + nome;
            lista += "\n|Peer: " + arqGlobais.get(nome);       
        }       
        return lista;    
    }
    
    public Address stringToAddress(String stringAddress){  
        for (Address a : view.getMembersRaw()) {   
            if (stringAddress.equals(a.toString()))
                return a;       
        }      
        return null;     
    }
    
    public void limparTela(){
        for (int i = 0; i < 30; i++) {
            System.out.println();
        }
    }

    public JChannel getChannel(){
        return channel;
    }

    public void setChannel(JChannel channel){
        this.channel = channel;
    }

    public View getView(){
        return view;
    }

    public void setView(View view){
        this.view = view;
    }

    public HashMap<String, String> getArqLocais(){
        return arqLocais;
    }

    public void setArqLocais(HashMap<String, String> arqLocais){
        this.arqLocais = arqLocais;
    }

    public HashMap<String, Address> getArqGlobais(){
        return arqGlobais;
    }

    public void setArqGlobais(HashMap<String, Address> arqGlobais){
        this.arqGlobais = arqGlobais;
    }
}