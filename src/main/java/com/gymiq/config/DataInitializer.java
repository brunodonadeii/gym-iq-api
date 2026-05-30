package com.gymiq.config;

import com.gymiq.entity.Exercise;
import com.gymiq.entity.User;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        criarAdminSeNaoExistir();
        criarExerciciosBaseSeNecessario();
    }

    private void criarAdminSeNaoExistir() {
        if (userRepository.existsByEmail("admin@gymiq.com")) {
            log.info("Usuário admin já existe — seed ignorado.");
            return;
        }

        User admin = User.builder()
                .name("Administrador GymIQ")
                .email("admin@gymiq.com")
                .passwordHash(passwordEncoder.encode("gymiq@2026"))
                .role(User.Role.ADMIN)
                .active(true)
                .lgpdAccepted(true)
                .build();

        userRepository.save(admin);

        log.info("=================================================");
        log.info("  Usuário admin criado com sucesso!");
        log.info("  Email : admin@gymiq.com");
        log.info("  Senha : gymiq@2026");
        log.info("=================================================");
    }

    private void criarExerciciosBaseSeNecessario() {
        List<SeedExercise> exerciciosBase = List.of(
                new SeedExercise("Supino reto com barra", "Peito", "Exercício composto para peitoral, ombros e tríceps."),
                new SeedExercise("Supino inclinado com halteres", "Peito", "Variação para enfatizar a porção superior do peitoral."),
                new SeedExercise("Crucifixo reto com halteres", "Peito", "Exercício de isolamento para peitoral com foco em amplitude."),
                new SeedExercise("Cross over na polia", "Peito", "Movimento de adução horizontal para peitoral."),
                new SeedExercise("Flexão de braços", "Peito", "Exercício com peso corporal para peitoral, ombros e tríceps."),

                new SeedExercise("Puxada frontal na polia", "Costas", "Exercício para dorsais e bíceps com pegada pronada."),
                new SeedExercise("Remada curvada com barra", "Costas", "Movimento composto para dorsais, romboides e lombar."),
                new SeedExercise("Remada baixa na polia", "Costas", "Exercício para espessura de costas e escápulas."),
                new SeedExercise("Barra fixa", "Costas", "Exercício com peso corporal para dorsais e bíceps."),
                new SeedExercise("Pulldown com braços estendidos", "Costas", "Isolamento para dorsais com foco em extensão do ombro."),

                new SeedExercise("Desenvolvimento com halteres", "Ombros", "Exercício composto para deltoides com foco anterior e medial."),
                new SeedExercise("Elevação lateral", "Ombros", "Isolamento para deltoide lateral."),
                new SeedExercise("Elevação frontal", "Ombros", "Isolamento para deltoide anterior."),
                new SeedExercise("Crucifixo invertido", "Ombros", "Isolamento para deltoide posterior."),
                new SeedExercise("Encolhimento com halteres", "Ombros", "Exercício para trapézio superior."),

                new SeedExercise("Rosca direta com barra", "Bíceps", "Exercício clássico para bíceps com pegada supinada."),
                new SeedExercise("Rosca alternada com halteres", "Bíceps", "Variação unilateral para bíceps."),
                new SeedExercise("Rosca martelo", "Bíceps", "Exercício para bíceps e braquial com pegada neutra."),
                new SeedExercise("Rosca concentrada", "Bíceps", "Isolamento para bíceps com foco em controle."),

                new SeedExercise("Tríceps pulley", "Tríceps", "Extensão de cotovelos na polia para tríceps."),
                new SeedExercise("Tríceps testa com barra", "Tríceps", "Exercício para cabeça longa do tríceps."),
                new SeedExercise("Tríceps banco", "Tríceps", "Exercício com peso corporal para tríceps."),
                new SeedExercise("Tríceps francês com halter", "Tríceps", "Extensão acima da cabeça para tríceps."),

                new SeedExercise("Agachamento livre", "Quadríceps", "Exercício composto para coxas e glúteos."),
                new SeedExercise("Leg press 45", "Quadríceps", "Exercício composto para quadríceps e glúteos."),
                new SeedExercise("Cadeira extensora", "Quadríceps", "Isolamento para quadríceps."),
                new SeedExercise("Afundo com halteres", "Quadríceps", "Exercício unilateral para pernas e glúteos."),
                new SeedExercise("Agachamento hack", "Quadríceps", "Variação guiada para quadríceps."),

                new SeedExercise("Mesa flexora", "Posterior de Coxa", "Isolamento para isquiotibiais."),
                new SeedExercise("Stiff com barra", "Posterior de Coxa", "Exercício para posteriores e glúteos com foco em hinge."),
                new SeedExercise("Levantamento terra romeno", "Posterior de Coxa", "Variação para posteriores e glúteos."),
                new SeedExercise("Glute bridge", "Posterior de Coxa", "Movimento para cadeia posterior com enfase em gluteos."),

                new SeedExercise("Elevação pélvica com barra", "Glúteos", "Exercício principal para glúteos."),
                new SeedExercise("Coice na polia", "Gluteos", "Isolamento para gluteos."),
                new SeedExercise("Abdução de quadril na máquina", "Glúteos", "Exercício para glúteo médio."),
                new SeedExercise("Passada andando", "Gluteos", "Exercicio dinamico para gluteos e quadriceps."),

                new SeedExercise("Panturrilha em pé", "Panturrilhas", "Exercício para gastrocnêmio."),
                new SeedExercise("Panturrilha sentada", "Panturrilhas", "Exercício para sóleo."),
                new SeedExercise("Panturrilha no leg press", "Panturrilhas", "Variação para panturrilhas com apoio no leg press."),

                new SeedExercise("Abdominal reto", "Abdômen", "Flexão de tronco com foco no reto abdominal."),
                new SeedExercise("Abdominal infra", "Abdômen", "Movimento para porção inferior do abdômen."),
                new SeedExercise("Prancha isométrica", "Abdômen", "Estabilização de core em isometria."),
                new SeedExercise("Abdominal bicicleta", "Abdômen", "Exercício dinâmico para core e oblíquos."),
                new SeedExercise("Elevação de pernas", "Abdômen", "Exercício para core com foco em controle lombar."),

                new SeedExercise("Levantamento terra", "Lombar", "Exercicio composto para cadeia posterior completa."),
                new SeedExercise("Hiperextensão lombar", "Lombar", "Exercício para eretores da espinha."),

                new SeedExercise("Bike ergométrica", "Cardio", "Atividade cardiovascular de baixo impacto."),
                new SeedExercise("Esteira", "Cardio", "Caminhada ou corrida para condicionamento cardiovascular."),
                new SeedExercise("Elíptico", "Cardio", "Atividade cardiovascular com baixo impacto articular."),
                new SeedExercise("Corda naval", "Cardio", "Exercício metabólico para condicionamento geral."),
                new SeedExercise("Burpee", "Cardio", "Exercício funcional de alta intensidade."),

                new SeedExercise("Remada alta com barra", "Trapézio", "Exercício para ombros e trapézio."),
                new SeedExercise("Face pull", "Trapézio", "Exercício para deltoide posterior e estabilizadores escapulares."),
                new SeedExercise("Farmer walk", "Antebraços", "Caminhada carregando pesos para pegada e core."),
                new SeedExercise("Rosca punho", "Antebraços", "Flexão de punhos para fortalecimento dos antebraços.")
        );

        int criados = 0;
        for (SeedExercise seedExercise : exerciciosBase) {
            if (exerciseRepository.existsByNameIgnoreCase(seedExercise.name())) {
                continue;
            }

            Exercise exercise = Exercise.builder()
                    .name(seedExercise.name())
                    .muscleGroup(seedExercise.muscleGroup())
                    .description(seedExercise.description())
                    .build();

            exerciseRepository.save(exercise);
            criados++;
        }

        if (criados > 0) {
            log.info("Seed de exercicios executado com sucesso. Exercicios criados={}", criados);
        } else {
            log.info("Seed de exercicios ignorado. Base ja possui os exercicios padrao.");
        }
    }

    private record SeedExercise(String name, String muscleGroup, String description) {
    }
}
