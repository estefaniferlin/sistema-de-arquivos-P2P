package util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase para representar as mensagens que ocorrem entre os peers
 * @author Estéfani Ferlin
 */
public class Mensagem implements Serializable {

    private String operacao;
    private Status status;

    /* 
     chave : Object
     */
    Map<String, String> params;

    /**
    * Método construtor da classe que inicializa os valores de params e operacao
    * @params operacao String
    */
    public Mensagem(String operacao) {
        this.operacao = operacao;
        params = new HashMap<>();
    }

    /**
    * Método construtor da classe que inicializa o valor de params
    */
    public Mensagem() {
        params = new HashMap<>();
    }
    
    /**
    * Método que retorna a operação
    * @return operacao
    */
    public String getOperacao() {
        return operacao;
    }

    /**
    * Método que atribui um valor para o status
    * @param s Status
    */
    public void setStatus(Status s) {
        this.status = s;
    }

    /**
    * Método que retorna o status
    * @return status
    */
    public Status getStatus() {
        return status;
    }

    /**
    * Método que atribui valores aos parametros chave e valor
    * @params chave String, valor String
    */
    public void setParam(String chave, String valor) {
        params.put(chave, valor);
    }

    /**
    * Método que retorna o valor da chave
    * @return chave
    */
    public String getParam(String chave) {
        return params.get(chave);
    }

    /**
    * Método que modifica um protocolo do tipo String para Mensagem
    * @params protocolo String
    * @return m
    */
    public static Mensagem parseString(String protocolo) {

        String p[] = protocolo.split(";");
        Mensagem m = new Mensagem(p[0]);
        try {
            
                for (int i = 1; i < p.length; i++) {
                    String chaveValor[] = p[i].split(":");
                    m.setParam(chaveValor[0], chaveValor[1]);
                }
            
        } catch (Exception e) {
            System.out.println("Falha no parser da mensagem: " + e.getMessage());
            return null;
        }
        return m;
    }

    /**
    * Método que passa valores para String
    * @return m
    */
    @Override
    public String toString() {
        String m = operacao;
        m += ";" + status;

        m += ";";
        for (String p : params.keySet()) {
            m += p + ": " + params.get(p) + ";";
        }
        return m;
    }

}
