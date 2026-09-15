package com.fiap.bank.atm;

import com.fiap.bank.atm.application.service.AtmService;
import com.fiap.bank.atm.presentation.AtmFrame;
import javax.swing.SwingUtilities;

public class AtmApplication {
    public static void main(String[] args) throws Exception {
        Class<?> repositoryType = Class.forName("com.fiap.bank.atm.domain.repository.AccountRepository");
        Object repository = Class.forName("com.fiap.bank.atm.infrastructure.persistence.AccountRepositoryJdbcImpl")
                .getDeclaredConstructor().newInstance();
        AtmService atmService = (AtmService) AtmService.class
                .getConstructor(repositoryType)
                .newInstance(repository);

        // Inicializa a camada de Apresentação de forma segura na Event Dispatch Thread
        // (EDT)
        SwingUtilities.invokeLater(() -> {
            AtmFrame mainFrame = new AtmFrame(atmService);
            mainFrame.setVisible(true);
        });
    }
}
