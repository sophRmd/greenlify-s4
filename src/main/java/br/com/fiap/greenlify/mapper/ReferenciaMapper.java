package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.models.*;

public class ReferenciaMapper {

    /**
     * Cria objetos "referência" (apenas com o id preenchido) a partir dos ids
     * recebidos nos DTOs de requisição. Os DAOs só precisam do id para gravar
     * as chaves estrangeiras, então o cliente nunca precisa (nem consegue)
     * enviar um objeto completo, evitando mass assignment de campos sensíveis.
     */
    private ReferenciaMapper() {
    }

    public static Usuario usuario(int id) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        return u;
    }

    public static UsuarioComum usuarioComum(int id) {
        UsuarioComum u = new UsuarioComum();
        u.setIdUsuario(id);
        return u;
    }

    public static Grupo grupo(int id) {
        Grupo g = new Grupo();
        g.setIdGrupo(id);
        return g;
    }

    public static Missao missao(int id) {
        Missao m = new Missao();
        m.setIdMissao(id);
        return m;
    }

    public static Pontuacao pontuacao(int id) {
        Pontuacao p = new Pontuacao();
        p.setIdPontuacao(id);
        return p;
    }

    public static ContaLuz contaLuz(int id) {
        ContaLuz c = new ContaLuz();
        c.setIdContaLuz(id);
        return c;
    }

    public static NivelContribuicao nivel(int id) {
        NivelContribuicao n = new NivelContribuicao();
        n.setIdNivel(id);
        return n;
    }

    public static Ranking ranking(int id) {
        Ranking r = new Ranking();
        r.setIdRanking(id);
        return r;
    }

    public static Recompensa recompensa(int id) {
        Recompensa r = new Recompensa();
        r.setIdRecompensa(id);
        return r;
    }

    public static Selo selo(int id) {
        Selo s = new Selo();
        s.setIdSelo(id);
        return s;
    }

    public static Upload upload(int id) {
        Upload u = new Upload();
        u.setIdUpload(id);
        return u;
    }

    public static Fingerprint fingerprint(int id) {
        Fingerprint f = new Fingerprint();
        f.setIdFingerprint(id);
        return f;
    }
}
