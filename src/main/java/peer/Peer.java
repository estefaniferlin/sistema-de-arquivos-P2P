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

/**
 * Classe que representa a comunicação entre os Peers
 * dentro do Sistema de Arquivos P2P. Essa classe possui
 * dois métodos principais eventLoop e receive que enviam
 * mensagens e recebem, respectivamente
 * 
 * @author Estéfani Ferlin
 */
public class Peer extends ReceiverAdapter {

    private JChannel channel;
    private View view;
    private boolean peerAutenticado;
    private HashMap<String, String> arqLocais;
    private HashMap<String, Address> arqGlobais;
    private HashMap<String, String> credenciais;

    /**
    * Método que inicia a comunicação entre os peers
    * pelo canal de Cluster chamado "Arquivos"
    */
    private void start() throws Exception {
    
        channel = new JChannel().setReceiver(this);
        channel.connect("Arquivos");    
        arqLocais = new HashMap<>();
        arqGlobais = new HashMap<>();      
        credenciais = new HashMap<>();
        credenciais.put("peer1", "1234");
        credenciais.put("peer2", "4321");
        credenciais.put("peer3", "1414");
        
        eventLoop(); 
        channel.close();
    }
    
    /**
    * Método para enviar os arquivos globais para o novo peer
    */
    private void sendInitializationMessage() throws Exception {
        Mensagem mensagem = new Mensagem("NOVO_PEER");
        channel.send(null, mensagem);
    }

    /**
    * Método que atualiza o view quando um peer entra ou sai da rede
    */
    @Override
    public void viewAccepted(View novaView) {
        this.view = novaView;
    }
    
    /**
    * Método que envia as mensagens dos peers para a rede. O usuário 
    * peer seleciona as opções que deseja realizar e são geradas as
    * mensagens para os outros peers. Para um usuário peer poder
    * interagir, ele precisa se autenticar, e quem realiza isso é 
    * o peer autenticador
    */
    private void eventLoop() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String nomeArq, conteudoArq, msgUser, nomeUsuario, senha;
        Mensagem mensagem;
        int opcao;
        Boolean continuar = true;
        peerAutenticado = false;
        
        if(this.channel.address().equals(view.getCoord())){
            System.out.println("\n----------< TORRENT DE ARQUIVOS P2P >----------\n");
            System.out.println("----------------PEER AUTENTICADOR----------------");
            System.out.println("> Aguardando pedidos para autenticar Peers...");
        }

        while(continuar){
            try {    
                if(!peerAutenticado && !this.channel.address().equals(view.getCoord())){
                    System.out.println("\n----------< TORRENT DE ARQUIVOS P2P >------------");
                    System.out.println("----------------ABA DE AUTENTICAÇÃO----------------");

                    System.out.println("Informe seu usuário: ");
                    nomeUsuario = in.readLine().toLowerCase();
                    System.out.println("Informe sua senha: ");
                    senha = in.readLine();

                    mensagem = new Mensagem("AUTENTICAR_PEER");
                    mensagem.setParam("nome_usuario", nomeUsuario);
                    mensagem.setParam("senha", senha);

                    channel.send(null, mensagem);

                    Thread.sleep(100);
                    
                    if(peerAutenticado){
                        sendInitializationMessage();
                        System.out.println("\n** Peer " + nomeUsuario + " foi autenticado com sucesso! **");
                    }else{
                        System.out.println("** Peer não foi autenticado, tente novamente! **");
                    }
                }else{
                    while (continuar && peerAutenticado && !this.channel.address().equals(view.getCoord())) {
                        System.out.println("\n----------< TORRENT DE ARQUIVOS P2P >------------");
                        System.out.println("------------------ABA DE OPÇÕES--------------------");
                        System.out.println("1 - Adicionar arquivo");
                        System.out.println("2 - Remover arquivo");
                        System.out.println("3 - Mostrar lista de arquivos locais");
                        System.out.println("4 - Mostrar lista de arquivos globais");    
                        System.out.println("5 - Visualizar conteúdo do arquivo");
                        System.out.println("6 - Mostrar os peers da rede");

                        System.out.println("\nOpção: ");
                        opcao = Integer.parseInt(in.readLine());

                        switch (opcao) {  
                            case 1: // Adicionar arquivo
                                System.out.println("\n> Informe o nome para o arquivo: ");
                                nomeArq = in.readLine().toLowerCase();
                                System.out.println("\n> Informe o conteúdo para o arquivo: ");
                                conteudoArq = in.readLine().toLowerCase();

                                if (arqGlobais.get(nomeArq) == null) {
                                    arqLocais.put(nomeArq, conteudoArq);
                                    arqGlobais.put(nomeArq, this.channel.address());
                                    mensagem = new Mensagem("NOVO_ARQUIVO");
                                    mensagem.setParam("nome_arquivo", nomeArq);
                                    channel.send(null, mensagem);   
                                    System.out.println("\nO arquivo " + nomeArq + " foi adicionado com sucesso!");     
                                } else
                                    System.out.println("\nO arquivo não pode ser criado pois já existe!");             
                            break;
                            case 2: // Deletar um arquivo                        
                                System.out.println("\n> Informe o nome do arquivo que deseja deletar: ");
                                nomeArq = in.readLine().toLowerCase();
                                if(arqLocais.get(nomeArq) != null){ 
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
                                if (arqLocais.get(nomeArq) != null) {   
                                    System.out.println("\nConteúdo: " + arqLocais.get(nomeArq));         
                                } else if(arqGlobais.get(nomeArq) != null) {
                                    mensagem = new Mensagem("SOLICITA_CONTEUDO_ARQUIVO");
                                    mensagem.setParam("nome_arquivo", nomeArq);
                                    channel.send(null, mensagem);                     
                                } else{
                                    System.out.println("\n** O arquivo não existe em nenhum Peer! **");
                                }
                            break;
                            case 6: // Mostrar os peers
                                System.out.println("\nEsses são os peers da rede: ");
                                System.out.println(view); 
                            break;
                            default:
                                System.out.println("\n** Opção inválida! **");
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
                                if(arqLocais != null){
                                    mensagem = new Mensagem("REMOVER_ARQUIVOS_PEER_SAIU");
                                    channel.send(null, mensagem);
                                }
                            }else{
                                System.out.println("\n** Opção inválida! **");
                                end = false;
                            }
                        }while(!end);
                    }
                }
            } catch (Exception e) {
                System.out.println("\n** Erro na comunicação com os peers! **");
                System.out.println("ERRO: " + e);
            }
        }
    }
 
    /**
    * Método que recebe as mensagens dos peers e trata as requisições
    */
    @Override
    public void receive(Message m) {
        Mensagem mensagem = (Mensagem) m.getObject();
        String nomeArq, nomeUsuario, senha;

        try {
            if(m.getSrc() != this.channel.address()){
                switch(mensagem.getOperacao()){
                    case "AUTENTICAR_PEER":
                        if(this.channel.address().equals(view.getCoord())){
                            nomeUsuario = mensagem.getParam("nome_usuario");
                            senha = mensagem.getParam("senha");
                            mensagem = new Mensagem("RESPOSTA_" + mensagem.getOperacao());
                            if(verificarCredenciais(nomeUsuario, senha)){
                                mensagem.setStatus(Status.OK);
                                peerAutenticado = true;
                            }else{
                                mensagem.setStatus(Status.ERROR);
                            }   
                            channel.send(m.getSrc(), mensagem);
                        }
                    break;
                    case "RESPOSTA_AUTENTICAR_PEER":
                        if(mensagem.getStatus() == Status.OK){
                            System.out.println("\nPeer autenticado");
                            peerAutenticado = true;
                        }           
                    break;
                    case "NOVO_PEER":
                        mensagem = new Mensagem("RESPOSTA_" + mensagem.getOperacao());
                        mensagem.setStatus(Status.OK);
                        String listaPeers = "";
                        for (String nome : arqGlobais.keySet()) {
                            listaPeers += nome + "=" + arqGlobais.get(nome) + ";"; // concatena o nome do arquivo e seu valor no formato "nome=valor;"
                        }
                        mensagem.setParam("lista_arquivos", listaPeers);                     
                        channel.send(m.getSrc(), mensagem);  
                    break;
                    case "RESPOSTA_NOVO_PEER":         
                        String listaArquivos[] = mensagem.getParam("lista_arquivos").split(";"); // obtém o valor associado à chave "lista_arquivos" na mensagem recebida          
                        for (String i : listaArquivos) {                 
                            String infosArquivo[] = i.split("=");
                            arqGlobais.put(infosArquivo[0], stringParaAddress(infosArquivo[1]));                      
                        }             
                    break;          
                    case "NOVO_ARQUIVO":          
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
                    break; 
                    case "REMOVER_ARQUIVOS_PEER_SAIU":
                        HashMap<String, Address> novoArquivosGlobais = new HashMap<>();
                        for(String nome : arqGlobais.keySet()){
                            if(!arqGlobais.get(nome).equals(m.getSrc())){ // se nao for referente ao peer que está saindo
                                novoArquivosGlobais.put(nome, arqGlobais.get(nome)); // informação dos peers que continuam na rede
                            }
                        }
                        arqGlobais.clear(); // apaga o que tinha no arquivo global, inclusive o que era do peer que está saindo
                        arqGlobais.putAll(novoArquivosGlobais); // adiciona as infos dos peers que continuam na rede
                    break;        
                }
            }
        } catch (Exception e) {
            //System.out.println("** Erro 2 na comunicação com os peers! **");
            //System.out.println("ERRO: " + e);
        }
    }
    
    /**
    * Método que mostra o nome e conteúdo de um arquivo na lista local do peer
    */
    public String listaArqLocais(){  
        String lista = "";
        
        for(String nome : arqLocais.keySet()){
            lista += "\n------------------------------------------------";
            lista += "\n|Nome: " + nome;
            lista += "\n|Conteúdo: " + arqLocais.get(nome);      
        }  
        return lista;    
    }
    
    /**
    * Método que mostra o nome e o peer que é dono de um arquivo na lista global
    */
    public String listaArqGlobais(){ 
        String lista = "";
        
        for(String nome : arqGlobais.keySet()){ 
            lista += "\n------------------------------------------------";
            lista += "\n|Nome: " + nome;
            lista += "\n|Peer: " + arqGlobais.get(nome);       
        }       
        return lista;    
    }
    
    /**
    * Método que transforma valores do tipo String para o tipo Address. É usado
    * nas mensagens que aceitam apenas o tipo String e então é necessário
    * realizar a conversão nos casos que se tem um Address do peer
    */
    public Address stringParaAddress(String string){  
        for (Address address : view.getMembersRaw()) {   
            if (string.equals(address.toString()))
                return address;       
        }      
        return null;     
    }
    
    /**
    * Método que limpa a tela para facilitar a visualização das informações.
    * É criado linhas que separam a resposta anterior da nova tela de menu
    * mostrada ao usuário peer
    */
    public void limparTela(){
        for (int i = 0; i < 30; i++) {
            System.out.println();
        }
    }
  
    /**
    * Método para retornar o channel do cluster de arquivos
    */
    public JChannel getChannel(){
        return channel;
    }

    /**
    * Método para atribuir um valor ao channel do cluster de arquivos
    */
    public void setChannel(JChannel channel){
        this.channel = channel;
    }
    
    /**
    * Método para retornar a view
    */
    public View getView(){
        return view;
    }

    /**
    * Método para atribuir um valor à view
    */
    public void setView(View view){
        this.view = view;
    }

    /**
    * Método para retornar as credenciais aceitas para a autenticação do peer
    */
    public HashMap<String, String> getCredenciais(){
        return credenciais;
    }

    /**
    * Método para setar credenciais válidas para a autenticação do peer
    */
    public void setCredenciais(HashMap<String, String> credenciais){
        this.credenciais = credenciais;
    }
    
    /**
    * Método para verificar se as credenciais informadas pelo peer que tenta
    * se autenticar condizem com as existentes dentro da Hash Map de credenciais
    * @param nomeUsuario String, senha String
    */
    public boolean verificarCredenciais(String nomeUsuario, String senha){
        boolean existe = false;
        if(credenciais.containsKey(nomeUsuario) && credenciais.get(nomeUsuario).equals(senha)){
          existe =  true;
        }
        return existe;
    }
    
    /**
    * Método main para inicializar o Peer
    * @params args
    * @throws Exception
    */
    public static void main(String[] args) throws Exception {
        new Peer().start();  
    }
}