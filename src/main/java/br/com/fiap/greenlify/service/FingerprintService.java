package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.FingerprintDAO;
import br.com.fiap.greenlify.dao.UploadDAO;
import br.com.fiap.greenlify.dto.FingerprintRequestDTO;
import br.com.fiap.greenlify.dto.FingerprintResponseDTO;
import br.com.fiap.greenlify.enums.StatusUploadEnum;
import br.com.fiap.greenlify.mapper.FingerprintMapper;
import br.com.fiap.greenlify.models.Fingerprint;
import br.com.fiap.greenlify.models.Upload;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FingerprintService {
    private final FingerprintDAO fingerprintDAO;
    private final UploadDAO uploadDAO;

    public FingerprintService() {
        this.fingerprintDAO = new FingerprintDAO();
        this.uploadDAO = new UploadDAO();
    }

    public FingerprintResponseDTO buscarPorId(int id) {
        Fingerprint fingerprint = fingerprintDAO.buscarPorId(id);
        if (fingerprint == null) {
            throw new IllegalArgumentException("Fingerprint não encontrada");
        }
        return FingerprintMapper.toDto(fingerprint);
    }

    public List<FingerprintResponseDTO> listar() {
        return fingerprintDAO.listar()
                .stream()
                .map(FingerprintMapper::toDto)
                .toList();
    }

    public void cadastrar(FingerprintRequestDTO dto) {
        validar(dto);
        // Um vídeo tem uma só fingerprint, e o mesmo hash não pode estar em outro upload.
        if (fingerprintDAO.existePorUpload(dto.idUpload())) {
            System.out.println("ERRO: O upload já tem fingerprint.");
            throw new IllegalArgumentException("O upload já tem uma fingerprint.");
        }
        validarHashSemDuplicidade(dto, dto.idUpload());
        fingerprintDAO.cadastrar(FingerprintMapper.dtoToEntity(dto));
    }

    public void excluir(int id) {
        Fingerprint fingerprint = fingerprintDAO.buscarPorId(id);
        if (fingerprint == null) {
            throw new IllegalArgumentException("Fingerprint não encontrada");
        }
        exigirUploadNaoConsolidado(fingerprint);
        fingerprintDAO.excluir(id);
    }

    public void alterar(FingerprintRequestDTO dto, int id) {
        Fingerprint existente = fingerprintDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Fingerprint não encontrada");
        }
        validar(dto);
        // A relação vídeo -> fingerprint é preservada: a fingerprint não muda de upload, e a de um
        // upload aprovado (comprovação já contabilizada) não é alterada.
        if (existente.getUpload() == null || existente.getUpload().getIdUpload() != dto.idUpload()) {
            System.out.println("ERRO: A fingerprint não pode mudar de upload.");
            throw new IllegalArgumentException("A fingerprint não pode ser movida para outro upload.");
        }
        exigirUploadNaoConsolidado(existente);
        validarHashSemDuplicidade(dto, dto.idUpload());

        Fingerprint fingerprint = FingerprintMapper.dtoToEntity(dto);
        fingerprint.setIdFingerprint(id);
        fingerprintDAO.alterar(fingerprint);
    }

    /** O mesmo hash de vídeo não pode pertencer a outro upload (seria burlar a verificação de duplicidade). */
    private void validarHashSemDuplicidade(FingerprintRequestDTO dto, int idUploadDono) {
        int idUploadComHash = fingerprintDAO.buscarIdUploadPorHash(dto.hashVideo());
        if (idUploadComHash > 0 && idUploadComHash != idUploadDono) {
            System.out.println("ERRO: Hash já registrado em outro upload.");
            throw new IllegalArgumentException("Esse hash de vídeo já está registrado no upload #" + idUploadComHash + ".");
        }
    }

    /**
     * Upload aprovado ou parcialmente aprovado já foi contabilizado (e serve de base para barrar
     * duplicidade): a fingerprint dele fica preservada, sem alteração nem exclusão.
     */
    private void exigirUploadNaoConsolidado(Fingerprint fingerprint) {
        if (fingerprint.getUpload() == null) {
            return;
        }
        Upload upload = uploadDAO.buscarPorId(fingerprint.getUpload().getIdUpload());
        if (upload != null && (upload.getStatus() == StatusUploadEnum.APROVADO
                || upload.getStatus() == StatusUploadEnum.PARCIALMENTE_APROVADO)) {
            System.out.println("ERRO: Fingerprint de upload " + upload.getStatus() + " é preservada.");
            throw new IllegalArgumentException(
                    "A fingerprint de um upload aprovado ou parcialmente aprovado é preservada e não pode ser alterada nem excluída.");
        }
    }

    private void validar(FingerprintRequestDTO dto) {
        if (dto.hashVideo() == null || dto.hashVideo().isBlank()) {
            System.out.println("ERRO: O hash do vídeo é obrigatório.");
            throw new IllegalArgumentException("O hash do vídeo é obrigatório.");
        }
        if (dto.dataGeracao() == null) {
            System.out.println("ERRO: A data de geração é obrigatória.");
            throw new IllegalArgumentException("A data de geração é obrigatória.");
        }
        if (dto.dataGeracao().isAfter(LocalDate.now())) {
            System.out.println("ERRO: A data de geração não pode ser futura.");
            throw new IllegalArgumentException("A data de geração não pode ser futura.");
        }
        if (dto.idUpload() <= 0) {
            System.out.println("ERRO: O upload é obrigatório.");
            throw new IllegalArgumentException("O upload é obrigatório.");
        }
        if (uploadDAO.buscarPorId(dto.idUpload()) == null) {
            System.out.println("ERRO: Upload informado não existe.");
            throw new IllegalArgumentException("O upload informado não existe.");
        }
    }
}