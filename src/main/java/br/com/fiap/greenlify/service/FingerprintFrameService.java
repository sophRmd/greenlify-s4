package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.FingerprintDAO;
import br.com.fiap.greenlify.dao.FingerprintFrameDAO;
import br.com.fiap.greenlify.dao.UploadDAO;
import br.com.fiap.greenlify.dto.FingerprintFrameRequestDTO;
import br.com.fiap.greenlify.dto.FingerprintFrameResponseDTO;
import br.com.fiap.greenlify.enums.StatusUploadEnum;
import br.com.fiap.greenlify.mapper.FingerprintFrameMapper;
import br.com.fiap.greenlify.models.Fingerprint;
import br.com.fiap.greenlify.models.FingerprintFrame;
import br.com.fiap.greenlify.models.Upload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FingerprintFrameService {
    private final FingerprintFrameDAO frameDAO;
    private final FingerprintDAO fingerprintDAO;
    private final UploadDAO uploadDAO;

    public FingerprintFrameService() {
        this.frameDAO = new FingerprintFrameDAO();
        this.fingerprintDAO = new FingerprintDAO();
        this.uploadDAO = new UploadDAO();
    }

    public FingerprintFrameResponseDTO buscarPorId(int id) {
        FingerprintFrame frame = frameDAO.buscarPorId(id);
        if (frame == null) {
            throw new IllegalArgumentException("Frame não encontrado");
        }
        return FingerprintFrameMapper.toDto(frame);
    }

    public List<FingerprintFrameResponseDTO> listar() {
        return frameDAO.listar()
                .stream()
                .map(FingerprintFrameMapper::toDto)
                .toList();
    }

    public List<FingerprintFrameResponseDTO> listarPorFingerprint(int idFingerprint) {
        return frameDAO.listarPorFingerprint(idFingerprint)
                .stream()
                .map(FingerprintFrameMapper::toDto)
                .toList();
    }

    public void cadastrar(FingerprintFrameRequestDTO dto) {
        validar(dto);
        exigirUploadNaoConsolidado(dto.idFingerprint());
        if (existeNumeroFrame(dto, null)) {
            System.out.println("ERRO: Já existe um frame com esse número nessa fingerprint.");
            throw new IllegalArgumentException("Já existe um frame com esse número nessa fingerprint.");
        }
        frameDAO.cadastrar(FingerprintFrameMapper.dtoToEntity(dto));
    }

    public void excluir(int id) {
        FingerprintFrame frame = frameDAO.buscarPorId(id);
        if (frame == null) {
            throw new IllegalArgumentException("Frame não encontrado");
        }
        exigirUploadNaoConsolidado(frame.getFingerprint().getIdFingerprint());
        frameDAO.excluir(id);
    }

    public void alterar(FingerprintFrameRequestDTO dto, int id) {
        FingerprintFrame existente = frameDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Frame não encontrado");
        }
        validar(dto);
        // O frame fica na fingerprint de origem, e o de upload aprovado não é alterado.
        if (existente.getFingerprint() == null
                || existente.getFingerprint().getIdFingerprint() != dto.idFingerprint()) {
            System.out.println("ERRO: O frame não pode mudar de fingerprint.");
            throw new IllegalArgumentException("O frame não pode ser movido para outra fingerprint.");
        }
        exigirUploadNaoConsolidado(dto.idFingerprint());
        if (existeNumeroFrame(dto, id)) {
            System.out.println("ERRO: Já existe um frame com esse número nessa fingerprint.");
            throw new IllegalArgumentException("Já existe um frame com esse número nessa fingerprint.");
        }

        FingerprintFrame frame = FingerprintFrameMapper.dtoToEntity(dto);
        frame.setIdFingerprintFrame(id);
        frameDAO.alterar(frame);
    }

    /**
     * Os frames de um upload aprovado ou parcialmente aprovado são a base da verificação de duplicidade
     * e da comprovação já contabilizada: ficam preservados (sem incluir, alterar nem excluir).
     */
    private void exigirUploadNaoConsolidado(int idFingerprint) {
        Fingerprint fingerprint = fingerprintDAO.buscarPorId(idFingerprint);
        if (fingerprint == null || fingerprint.getUpload() == null) {
            return;
        }
        Upload upload = uploadDAO.buscarPorId(fingerprint.getUpload().getIdUpload());
        if (upload != null && (upload.getStatus() == StatusUploadEnum.APROVADO
                || upload.getStatus() == StatusUploadEnum.PARCIALMENTE_APROVADO)) {
            System.out.println("ERRO: Frames de upload " + upload.getStatus() + " são preservados.");
            throw new IllegalArgumentException(
                    "Os frames de um upload aprovado ou parcialmente aprovado são preservados e não podem ser alterados.");
        }
    }

    private boolean existeNumeroFrame(FingerprintFrameRequestDTO dto, Integer idIgnorado) {
        return frameDAO.listarPorFingerprint(dto.idFingerprint())
                .stream()
                .anyMatch(f -> f.getNumeroFrame() == dto.numeroFrame()
                        && (idIgnorado == null || f.getIdFingerprintFrame() != idIgnorado));
    }

    private void validar(FingerprintFrameRequestDTO dto) {
        if (dto.numeroFrame() < 0) {
            System.out.println("ERRO: O número do frame não pode ser negativo.");
            throw new IllegalArgumentException("O número do frame não pode ser negativo.");
        }
        if (dto.tempoSegundo() < 0) {
            System.out.println("ERRO: O tempo em segundos não pode ser negativo.");
            throw new IllegalArgumentException("O tempo em segundos não pode ser negativo.");
        }
        if (dto.pHash() == null || dto.pHash().isBlank()) {
            System.out.println("ERRO: O pHash é obrigatório.");
            throw new IllegalArgumentException("O pHash é obrigatório.");
        }
        if (dto.embedding() == null || dto.embedding().isBlank()) {
            System.out.println("ERRO: O embedding é obrigatório.");
            throw new IllegalArgumentException("O embedding é obrigatório.");
        }
        if (dto.idFingerprint() <= 0) {
            System.out.println("ERRO: A fingerprint é obrigatória.");
            throw new IllegalArgumentException("A fingerprint é obrigatória.");
        }
        if (fingerprintDAO.buscarPorId(dto.idFingerprint()) == null) {
            System.out.println("ERRO: Fingerprint informada não existe.");
            throw new IllegalArgumentException("A fingerprint informada não existe.");
        }
    }
}