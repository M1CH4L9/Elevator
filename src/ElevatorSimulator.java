import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Timer;
import javax.swing.*;

public class ElevatorSimulator extends JFrame {
    //stałe
    private static final int TOTAL_FLOORS = 11;
    private static final int MAX_CAPACITY = 5;
    private static final Color CABIN_COLOR = Color.LIGHT_GRAY;
    private static final Color PASSENGER_COLOR = Color.BLACK;
    private static final Color BUTTON_ACTIVE = Color.GREEN;
    private static final Color BUTTON_INACTIVE = Color.LIGHT_GRAY;
    private static final int MOVEMENT_DELAY = 30;
    private static final double MOVEMENT_SPEED = 0.03;

    //gui
    private JPanel mainPanel;
    private JPanel elevatorShaft;
    private JPanel controlPanel;
    private JPanel[] floorPanels;
    private JButton startSimulationButton;
    private JButton[] destinationButtons;
    private JButton[] callButtons;
    private JLabel[] directionIndicators;

    //stan
    private boolean simulationActive = false;
    private double cabinPosition = 0.0;
    private int currentFloor = 0;
    private int passengersInside = 0;
    private int[] waitingPassengers;
    private boolean[] selectedDestinations;
    private boolean[] floorRequests;
    private boolean movingUp = true;
    private Timer movementTimer;
    private Timer waitTimer;
    private Timer endTimer;
    private boolean elevatorStopped = false;

    public ElevatorSimulator() {
        setTitle("Symulator Windy");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //N/S/E/W/C
        setLayout(new BorderLayout());

        initializeData();
        createGUI();
        setupInputListeners(); //inputy m/k

        pack(); //robi dobry rozmiar okna do komponentów
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeData() {
        waitingPassengers = new int[TOTAL_FLOORS];
        selectedDestinations = new boolean[TOTAL_FLOORS];
        floorRequests = new boolean[TOTAL_FLOORS];
        floorPanels = new JPanel[TOTAL_FLOORS];
        callButtons = new JButton[TOTAL_FLOORS];
        directionIndicators = new JLabel[TOTAL_FLOORS];
        destinationButtons = new JButton[TOTAL_FLOORS];
    }

    private void createGUI() {
        mainPanel = new JPanel(new BorderLayout());

        createElevatorShaft();
        createControlPanel();
        createCallPanels();
        createStartButton();

        add(mainPanel, BorderLayout.CENTER);
        add(startSimulationButton, BorderLayout.SOUTH);
    }

    private void createElevatorShaft() {
        elevatorShaft = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawElevatorShaft(g);
            }
        };
        elevatorShaft.setPreferredSize(new Dimension(300, 600));
        elevatorShaft.setBackground(Color.WHITE);
        mainPanel.add(elevatorShaft, BorderLayout.CENTER);
    }

    private void createControlPanel() {
        controlPanel = new JPanel(new GridLayout(4, 3, 2, 2));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Panel windy"));
        controlPanel.setPreferredSize(new Dimension(120, 160));

        // tworzenie nierównych przycisków
        destinationButtons[9] = createDestinationButton("9", 9);
        destinationButtons[10] = createDestinationButton("10", 10);
        controlPanel.add(destinationButtons[9]);
        controlPanel.add(destinationButtons[10]);
        controlPanel.add(new JLabel());

        for (int i = 6; i <= 8; i++) {
            destinationButtons[i] = createDestinationButton(String.valueOf(i), i);
            controlPanel.add(destinationButtons[i]);
        }

        for (int i = 3; i <= 5; i++) {
            destinationButtons[i] = createDestinationButton(String.valueOf(i), i);
            controlPanel.add(destinationButtons[i]);
        }

        for (int i = 0; i <= 2; i++) {
            destinationButtons[i] = createDestinationButton(String.valueOf(i), i);
            controlPanel.add(destinationButtons[i]);
        }

        mainPanel.add(controlPanel, BorderLayout.WEST);
    }

    private JButton createDestinationButton(String text, int floor) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(30, 30));
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setMargin(new Insets(2, 2, 2, 2));
        button.addActionListener(e -> selectDestination(floor));
        button.setEnabled(false);
        button.setFocusPainted(false);
        return button;
    }

    private void createCallPanels() {
        //grid layout dzieli panel
        JPanel rightPanel = new JPanel(new GridLayout(TOTAL_FLOORS, 1, 0, 0));
        rightPanel.setPreferredSize(new Dimension(80, 600));

        //tworzenie panelu przywoływania
        for (int floor = TOTAL_FLOORS - 1; floor >= 0; floor--) {
            JPanel floorPanel = new JPanel(new BorderLayout());
            floorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            floorPanel.setPreferredSize(new Dimension(80, 54));

            directionIndicators[floor] = new JLabel("", SwingConstants.CENTER);
            directionIndicators[floor].setFont(new Font("Arial", Font.BOLD, 16));
            directionIndicators[floor].setPreferredSize(new Dimension(80, 25));
            floorPanel.add(directionIndicators[floor], BorderLayout.NORTH);

            callButtons[floor] = new JButton("");
            callButtons[floor].setPreferredSize(new Dimension(80, 25));
            callButtons[floor].setBackground(Color.BLACK);
            callButtons[floor].setForeground(Color.BLACK);
            callButtons[floor].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            callButtons[floor].setFocusPainted(false);
            callButtons[floor].setContentAreaFilled(true);
            final int f = floor;
            callButtons[floor].addActionListener(e -> callElevator(f));
            callButtons[floor].setEnabled(false);
            floorPanel.add(callButtons[floor], BorderLayout.SOUTH);

            floorPanels[floor] = floorPanel;
            rightPanel.add(floorPanel);
        }

        mainPanel.add(rightPanel, BorderLayout.EAST);
    }

    private void createStartButton() {
        startSimulationButton = new JButton("START");
        startSimulationButton.setPreferredSize(new Dimension(500, 40));
        startSimulationButton.addActionListener(e -> startSimulation());
    }

    private void drawElevatorShaft(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int shaftWidth = elevatorShaft.getWidth();
        int shaftHeight = elevatorShaft.getHeight();
        int floorHeight = shaftHeight / TOTAL_FLOORS;

        // rysowanie
        for (int i = 0; i < TOTAL_FLOORS; i++) {
            int y = shaftHeight - (i + 1) * floorHeight;

            g2d.drawLine(0, y, shaftWidth, y);
            g2d.drawString(String.valueOf(i), 5, y + floorHeight - 5);

            // rysowanie pasażerów na
            if (waitingPassengers[i] > 0) {
                for (int j = 0; j < waitingPassengers[i]; j++) {
                    g2d.setColor(PASSENGER_COLOR);
                    g2d.fillOval(shaftWidth - 165 + j * 15, y + floorHeight - 25, 10, 10);
                }
                g2d.setColor(Color.BLACK);
            }
        }

        // pozycja kabiny, cabinPosition - pozycja kabiny jako numer piętra
        int elevatorY = (int)(shaftHeight - floorHeight - (cabinPosition * floorHeight)) + 5;
        elevatorY = Math.max(5, Math.min(elevatorY, shaftHeight - floorHeight + 5));

        // rysowanie kabiny
        g2d.setColor(CABIN_COLOR);
        g2d.fillRect(50, elevatorY, 80, floorHeight - 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(50, elevatorY, 80, floorHeight - 10);

        // rysowanie pasażerów
        if (passengersInside > 0) {
            for (int i = 0; i < passengersInside; i++) {
                g2d.setColor(PASSENGER_COLOR);
                //55 przesunięcie od kabiny
                //i % 3 pozycja w rzędzie
                //* 15 odstęp 15 px między pasażerami
                //elevatorY + 10 pozycja y od góry kabiny
                //i / 3 numer rzędu
                g2d.fillOval(55 + (i % 3) * 15, elevatorY + 10 + (i / 3) * 15, 10, 10);
            }
        }
    }

    private void setupInputListeners() {
        // wypuszczanie pasażerów
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_E && simulationActive &&
                        passengersInside > 0 && elevatorStopped) {
                    removePassenger();
                }
            }
        });
        setFocusable(true);
        requestFocus();

        //focus po kliknięciu, żeby działało
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocus();
            }
        });
    }

    private void startSimulation() {
        simulationActive = true;
        startSimulationButton.setEnabled(false);

        if (endTimer != null) {
            endTimer.cancel();
            endTimer = null;
        }

        // losowanie pasażerów
        Random random = new Random();
        for (int i = 0; i < TOTAL_FLOORS; i++) {
            waitingPassengers[i] = random.nextInt(6);
        }

        // reset
        currentFloor = 0;
        cabinPosition = 0.0;
        passengersInside = 0;
        Arrays.fill(selectedDestinations, false);
        Arrays.fill(floorRequests, false);
        movingUp = true;
        elevatorStopped = true;

        updateCallButtons();
        repaint();

        // timer żeby winda była płynna
        movementTimer = new Timer();
        movementTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                moveElevator();
            }
        }, 50, MOVEMENT_DELAY);

        startWaitTimer();
    }

    private void updateCallButtons() {
        for (int i = 0; i < TOTAL_FLOORS; i++) {
            // aktywacja przycisku w dobrym momencie
            callButtons[i].setEnabled(waitingPassengers[i] > 0 && !floorRequests[i] && simulationActive);

            // zmiana koloru przycisku wezwania
            if (floorRequests[i]) {
                callButtons[i].setBackground(Color.GREEN);
                callButtons[i].setBorder(BorderFactory.createLineBorder(Color.GREEN));
            } else {
                callButtons[i].setBackground(Color.BLACK);
                callButtons[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            }

            if (simulationActive) {
                directionIndicators[i].setText(movingUp ? "↑" : "↓");
            }
            else {
                directionIndicators[i].setText("");
            }
        }
    }

    private void callElevator(int floor) {
        if (waitingPassengers[floor] > 0 && simulationActive) {
            floorRequests[floor] = true;
            callButtons[floor].setEnabled(false);

            //czy ma jechać od razu, czy ma czekać 5s po interakcji nawet
//            if (elevatorStopped) {
//                elevatorStopped = false;
//            }

            if (endTimer != null) {
                endTimer.cancel();
                endTimer = null;
            }
        }
    }

    private void selectDestination(int floor) {
        if (passengersInside > 0 && simulationActive) {
            selectedDestinations[floor] = true;
            destinationButtons[floor].setEnabled(false);
            destinationButtons[floor].setBackground(BUTTON_ACTIVE);

            //czy ma jechać od razu, czy ma czekać 5s po interakcji nawet
//            if (elevatorStopped) {
//                elevatorStopped = false;
//            }

            if (endTimer != null) {
                endTimer.cancel();
                endTimer = null;
            }
        }
    }

    private void updateDestinationButtons() {
        boolean hasPassengers = passengersInside > 0;
        for (int i = 0; i < TOTAL_FLOORS; i++) {
            destinationButtons[i].setEnabled(hasPassengers && !selectedDestinations[i] && simulationActive);
            destinationButtons[i].setBackground(selectedDestinations[i] ? BUTTON_ACTIVE : BUTTON_INACTIVE);
        }
    }

    private void moveElevator() {
        if (!simulationActive) return;

        SwingUtilities.invokeLater(() -> {
            updateCallButtons();
            repaint();
        });

        // sprawda piętro
        if (shouldStop()) {
            stopElevator();
            return;
        }

        // rusza winde
        if (!elevatorStopped && hasPendingRequests()) {
            moveCabin();
        }
    }

    private boolean shouldStop() {
        int nearestFloor = (int) Math.round(cabinPosition);
        // abs zwraca moduł
        double distance = Math.abs(cabinPosition - nearestFloor);
        boolean closeEnough = distance < 0.1;
        //sprawdzamy czy winda jest w 10% wysokości piętra
        boolean shouldStop = floorRequests[nearestFloor] || selectedDestinations[nearestFloor];

        if (closeEnough && shouldStop) {
            cabinPosition = nearestFloor;
            currentFloor = nearestFloor;
            return true;
        }

        return false;
    }

    private void moveCabin() {
        // rusza jeśli ma cel w toą samą stronę
        if (hasRequestsInDirection()) {
            if (movingUp) {
                cabinPosition += MOVEMENT_SPEED;
                cabinPosition = Math.min(cabinPosition, TOTAL_FLOORS - 1);
            } else {
                cabinPosition -= MOVEMENT_SPEED;
                cabinPosition = Math.max(cabinPosition, 0);
            }
        }
        // zmiana kierunku
        else if (hasPendingRequests()) {
            movingUp = !movingUp;
            if (movingUp) {
                cabinPosition += MOVEMENT_SPEED;
            } else {
                cabinPosition -= MOVEMENT_SPEED;
            }
        }
        // zatrzymuje
        else {
            elevatorStopped = true;
        }

        currentFloor = (int) Math.round(cabinPosition);
    }

    private boolean hasPendingRequests() {
        for (int i = 0; i < TOTAL_FLOORS; i++) {
            if (floorRequests[i] || selectedDestinations[i]) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRequestsInDirection() {
        for (int i = 0; i < TOTAL_FLOORS; i++) {
            if (floorRequests[i] || selectedDestinations[i]) {
                if (movingUp && i > cabinPosition) return true;
                if (!movingUp && i < cabinPosition) return true;
            }
        }
        return false;
    }

    private void stopElevator() {
        elevatorStopped = true;
        cabinPosition = currentFloor;

        if (selectedDestinations[currentFloor]) {
            selectedDestinations[currentFloor] = false;
            destinationButtons[currentFloor].setBackground(BUTTON_INACTIVE);
        }

        // wsiadanie
        if (floorRequests[currentFloor]) {
            int availableSpace = MAX_CAPACITY - passengersInside;
            int entering = Math.min(waitingPassengers[currentFloor], availableSpace);
            passengersInside += entering;
            waitingPassengers[currentFloor] -= entering;
            floorRequests[currentFloor] = false;
        }

        updateCallButtons();
        updateDestinationButtons();
        startWaitTimer();
    }

    private void startWaitTimer() {
        if (waitTimer != null) {
            waitTimer.cancel();
        }

        waitTimer = new Timer();
        waitTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (elevatorStopped && hasPendingRequests()) {
                    elevatorStopped = false;
                }
                checkEndConditions();
            }
        }, 5000);
    }

    private void removePassenger() {
        if (passengersInside > 0 && elevatorStopped) {
            passengersInside--;
            updateDestinationButtons();
            repaint();
            checkEndConditions();
        }
    }

    private void checkEndConditions() {
        boolean noPassengers = (passengersInside == 0);
        boolean noRequests = true;
        boolean isStopped = elevatorStopped;

        for (int i = 0; i < TOTAL_FLOORS; i++) {
            if (floorRequests[i] || selectedDestinations[i]) {
                noRequests = false;
                break;
            }
        }

        // timer konca
        if (isStopped && noPassengers && noRequests) {
            if (endTimer == null) {
                endTimer = new Timer();
                endTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (shouldEndSimulation()) {
                            endSimulation();
                        }
                        endTimer = null;
                    }
                }, 10000);
            }
        } else {
            if (endTimer != null) {
                endTimer.cancel();
                endTimer = null;
            }
        }
    }

    private boolean shouldEndSimulation() {
        return elevatorStopped && passengersInside == 0 && !hasPendingRequests();
    }

    private void endSimulation() {
        simulationActive = false;
        if (movementTimer != null) movementTimer.cancel();
        if (waitTimer != null) waitTimer.cancel();
        if (endTimer != null) {
            endTimer.cancel();
            endTimer = null;
        }

        SwingUtilities.invokeLater(() -> {
            startSimulationButton.setEnabled(true);
            for (int i = 0; i < TOTAL_FLOORS; i++) {
                destinationButtons[i].setEnabled(false);
                callButtons[i].setEnabled(false);
                destinationButtons[i].setBackground(BUTTON_INACTIVE);
                callButtons[i].setBackground(Color.BLACK);
                callButtons[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                directionIndicators[i].setText("");
            }
            repaint();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ElevatorSimulator();
        });
    }
}