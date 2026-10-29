package com.library.fx;

import com.library.dao.*;
import com.library.model.*;
import com.library.service.CatalogService;
import com.library.service.LoanService;
import com.library.util.ValidationUtil;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JavaFX Desktop Admin Console connecting directly to the SQLite shared DAO layer.
 * Demonstrates advanced GUI programming, property bindings, event handlers,
 * FilteredList/SortedList live filtering, Charts, and Dialogs.
 *
 * [Concept: Advanced GUI Programming with JavaFX]
 * [Concept: ObservableList and Property Bindings]
 * [Concept: Live Search Filtering with FilteredList and SortedList]
 * [Concept: JavaFX Event Handling - ActionEvent, MouseEvent, KeyEvent, Listeners]
 * [Concept: JavaFX UI Components - TabPane, TableView, ComboBox, DatePicker, BarChart, Alert]
 */
public class AdminApp extends Application {

    private BookDAO bookDAO;
    private MemberDAO memberDAO;
    private TransactionDAO transactionDAO;
    private ReservationDAO reservationDAO;
    private CatalogService catalogService;
    private LoanService loanService;

    // [Concept: ObservableList for Reactive TableView Data]
    private final ObservableList<LibraryItem> masterBookList = FXCollections.observableArrayList();
    private final ObservableList<Member> masterMemberList = FXCollections.observableArrayList();
    private final ObservableList<Transaction> masterLoanList = FXCollections.observableArrayList();

    private TableView<LibraryItem> bookTable;
    private TableView<Member> memberTable;
    private TableView<Transaction> loanTable;
    private BarChart<String, Number> categoryChart;

    @Override
    public void init() {
        // Initialize database and core DAOs (shared architecture)
        DatabaseManager.initializeDatabase();
        this.bookDAO = new BookDAO();
        this.memberDAO = new MemberDAO();
        this.transactionDAO = new TransactionDAO();
        this.reservationDAO = new ReservationDAO();
        this.catalogService = new CatalogService(bookDAO);
        this.loanService = new LoanService(transactionDAO, bookDAO, memberDAO, reservationDAO);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Athenaeum Library System — Desktop Administration");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // Top Header
        VBox header = createHeader();
        root.setTop(header);

        // Center TabPane
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("main-tabpane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab booksTab = new Tab("Catalog Holdings", createBooksTabView());
        Tab membersTab = new Tab("Members Management", createMembersTabView());
        Tab circulationTab = new Tab("Issue & Return Desk", createCirculationTabView());
        Tab overdueTab = new Tab("Overdue & Penalties", createOverdueTabView());
        Tab analyticsTab = new Tab("Analytics & Charts", createAnalyticsTabView());

        tabPane.getTabs().addAll(booksTab, membersTab, circulationTab, overdueTab, analyticsTab);
        root.setCenter(tabPane);

        // Initial Data Load
        refreshBookData();
        refreshMemberData();
        refreshLoanData();

        Scene scene = new Scene(root, 1100, 720);

        // Attach stylesheet
        String cssPath = getClass().getResource("/com/library/fx/javafx.css") != null
                ? getClass().getResource("/com/library/fx/javafx.css").toExternalForm()
                : null;
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    private VBox createHeader() {
        VBox box = new VBox(4);
        box.getStyleClass().add("app-header");
        box.setPadding(new Insets(16, 24, 16, 24));

        Label brand = new Label("ATHENAEUM");
        brand.getStyleClass().add("header-brand");

        Label subtitle = new Label("Desktop Admin Console — Shared SQLite Architecture");
        subtitle.getStyleClass().add("header-subtitle");

        box.getChildren().addAll(brand, subtitle);
        return box;
    }

    // =========================================================================
    // Tab 1: Books Catalog (TableView, FilteredList, SortedList, Events)
    // =========================================================================
    private Node createBooksTabView() {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16));

        // Search Bar & Actions Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label searchLbl = new Label("Filter Catalog:");
        TextField searchField = new TextField();
        searchField.setPromptText("Type title, author, genre or ISBN...");
        searchField.setPrefWidth(320);

        Button btnAddBook = new Button("+ Add New Holding");
        btnAddBook.getStyleClass().add("btn-primary");

        Button btnRefresh = new Button("↻ Refresh");
        Button btnDelete = new Button("Delete Selected");
        btnDelete.getStyleClass().add("btn-danger");

        toolbar.getChildren().addAll(searchLbl, searchField, btnAddBook, btnRefresh, btnDelete);

        // TableView
        bookTable = new TableView<>();
        bookTable.setPlaceholder(new Label("No catalog holdings found"));

        TableColumn<LibraryItem, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        colId.setPrefWidth(50);

        TableColumn<LibraryItem, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemType().getDisplayName()));
        colType.setPrefWidth(110);

        TableColumn<LibraryItem, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        colTitle.setPrefWidth(260);

        TableColumn<LibraryItem, String> colDetails = new TableColumn<>("Details");
        colDetails.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDisplayDetails()));
        colDetails.setPrefWidth(320);

        TableColumn<LibraryItem, Number> colYear = new TableColumn<>("Year");
        colYear.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getYear()));
        colYear.setPrefWidth(70);

        TableColumn<LibraryItem, String> colCopies = new TableColumn<>("Copies");
        colCopies.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getAvailableCopies() + " / " + cell.getValue().getTotalCopies()
        ));
        colCopies.setPrefWidth(90);

        bookTable.getColumns().addAll(colId, colType, colTitle, colDetails, colYear, colCopies);
        VBox.setVgrow(bookTable, Priority.ALWAYS);

        // [Concept: FilteredList and SortedList for dynamic search filtering]
        FilteredList<LibraryItem> filteredBooks = new FilteredList<>(masterBookList, p -> true);

        // [Concept: Event Handling - Listener on search field text property]
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredBooks.setPredicate(item -> {
                if (newValue == null || newValue.isBlank()) return true;
                return item.matchesQuery(newValue);
            });
        });

        // [Concept: SortedList bound to TableView comparator]
        SortedList<LibraryItem> sortedBooks = new SortedList<>(filteredBooks);
        sortedBooks.comparatorProperty().bind(bookTable.comparatorProperty());
        bookTable.setItems(sortedBooks);

        // [Concept: Event Handling - ActionEvent on Button]
        btnAddBook.setOnAction(e -> showAddHoldingDialog());
        btnRefresh.setOnAction(e -> refreshBookData());
        btnDelete.setOnAction(e -> deleteSelectedBook());

        // [Concept: Event Handling - KeyEvent on TableView (Delete key)]
        bookTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                deleteSelectedBook();
            }
        });

        // [Concept: Event Handling - MouseEvent double-click row]
        bookTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && bookTable.getSelectionModel().getSelectedItem() != null) {
                LibraryItem selected = bookTable.getSelectionModel().getSelectedItem();
                showInfoAlert("Holding Details", selected.getTitle(),
                        selected.getDisplayDetails() + "\nCopies: " + selected.getAvailableCopies() + "/" + selected.getTotalCopies());
            }
        });

        layout.getChildren().addAll(toolbar, bookTable);
        return layout;
    }

    // =========================================================================
    // Tab 2: Members Management
    // =========================================================================
    private Node createMembersTabView() {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16));

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnAddMember = new Button("+ Add New Member");
        btnAddMember.getStyleClass().add("btn-primary");
        Button btnRefresh = new Button("↻ Refresh Members");

        toolbar.getChildren().addAll(btnAddMember, btnRefresh);

        memberTable = new TableView<>();
        memberTable.setPlaceholder(new Label("No members registered yet"));

        TableColumn<Member, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colId.setPrefWidth(50);

        TableColumn<Member, String> colMemId = new TableColumn<>("Member ID");
        colMemId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMembershipId()));
        colMemId.setPrefWidth(130);

        TableColumn<Member, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colName.setPrefWidth(200);

        TableColumn<Member, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEmail.setPrefWidth(220);

        TableColumn<Member, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        colPhone.setPrefWidth(140);

        TableColumn<Member, String> colQuota = new TableColumn<>("Active Loans / Limit");
        colQuota.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getActiveBorrowCount() + " / " + c.getValue().getMaxBorrowLimit()
        ));
        colQuota.setPrefWidth(160);

        memberTable.getColumns().addAll(colId, colMemId, colName, colEmail, colPhone, colQuota);
        VBox.setVgrow(memberTable, Priority.ALWAYS);

        memberTable.setItems(masterMemberList);

        btnAddMember.setOnAction(e -> showAddMemberDialog());
        btnRefresh.setOnAction(e -> refreshMemberData());

        layout.getChildren().addAll(toolbar, memberTable);
        return layout;
    }

    // =========================================================================
    // Tab 3: Issue & Return Desk (ComboBox, DatePicker, Forms with Validation)
    // =========================================================================
    private Node createCirculationTabView() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        // Card 1: Issue Book Form
        VBox issueCard = new VBox(12);
        issueCard.getStyleClass().add("card-pane");
        issueCard.setPadding(new Insets(16));

        Label issueHeader = new Label("Issue Book to Member");
        issueHeader.getStyleClass().add("card-header");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        // [Concept: ComboBox UI Component]
        ComboBox<Member> comboMember = new ComboBox<>(masterMemberList);
        comboMember.setPromptText("Select Patron...");
        comboMember.setPrefWidth(280);
        comboMember.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getMembershipId() + ")");
            }
        });
        comboMember.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getMembershipId() + ")");
            }
        });

        // Filterable books list
        ComboBox<LibraryItem> comboBook = new ComboBox<>(masterBookList);
        comboBook.setPromptText("Select Available Book...");
        comboBook.setPrefWidth(380);
        comboBook.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LibraryItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle() + " [Available: " + item.getAvailableCopies() + "]");
            }
        });
        comboBook.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LibraryItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle() + " [Available: " + item.getAvailableCopies() + "]");
            }
        });

        // [Concept: DatePicker UI Component]
        DatePicker dateDue = new DatePicker(LocalDate.now().plusDays(14));
        dateDue.setPrefWidth(180);

        Button btnIssue = new Button("Confirm Issue");
        btnIssue.getStyleClass().add("btn-primary");

        grid.add(new Label("Select Member:"), 0, 0);
        grid.add(comboMember, 1, 0);
        grid.add(new Label("Select Item:"), 0, 1);
        grid.add(comboBook, 1, 1);
        grid.add(new Label("Due Date:"), 0, 2);
        grid.add(dateDue, 1, 2);
        grid.add(btnIssue, 1, 3);

        issueCard.getChildren().addAll(issueHeader, grid);

        // Card 2: Quick Return Table
        VBox returnCard = new VBox(12);
        returnCard.getStyleClass().add("card-pane");
        returnCard.setPadding(new Insets(16));
        VBox.setVgrow(returnCard, Priority.ALWAYS);

        HBox returnHeaderBox = new HBox(12);
        returnHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label returnHeader = new Label("Active Loans (Select Row to Process Return)");
        returnHeader.getStyleClass().add("card-header");
        Button btnProcessReturn = new Button("✔ Return Selected Loan");
        btnProcessReturn.getStyleClass().add("btn-primary");
        Button btnMarkLost = new Button("⚠ Mark Selected as Lost");
        btnMarkLost.getStyleClass().add("btn-danger");
        returnHeaderBox.getChildren().addAll(returnHeader, btnProcessReturn, btnMarkLost);

        loanTable = new TableView<>();
        loanTable.setPlaceholder(new Label("No active loans"));

        TableColumn<Transaction, Number> colTxId = new TableColumn<>("Tx #");
        colTxId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colTxId.setPrefWidth(55);

        TableColumn<Transaction, String> colTxBook = new TableColumn<>("Book Title");
        colTxBook.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemTitle()));
        colTxBook.setPrefWidth(260);

        TableColumn<Transaction, String> colTxMember = new TableColumn<>("Member");
        colTxMember.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMemberName()));
        colTxMember.setPrefWidth(180);

        TableColumn<Transaction, String> colTxDue = new TableColumn<>("Due Date");
        colTxDue.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDueDate()));
        colTxDue.setPrefWidth(110);

        TableColumn<Transaction, String> colTxStatus = new TableColumn<>("Status");
        colTxStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getLabel()));
        colTxStatus.setPrefWidth(100);

        loanTable.getColumns().addAll(colTxId, colTxBook, colTxMember, colTxDue, colTxStatus);
        VBox.setVgrow(loanTable, Priority.ALWAYS);
        loanTable.setItems(masterLoanList);

        returnCard.getChildren().addAll(returnHeaderBox, loanTable);

        // Actions
        btnIssue.setOnAction(e -> {
            Member member = comboMember.getValue();
            LibraryItem item = comboBook.getValue();
            LocalDate dueDate = dateDue.getValue();

            if (member == null || item == null || dueDate == null) {
                showErrorAlert("Validation Error", "Please choose both member and holding, and specify a valid due date.");
                return;
            }

            try {
                long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
                Transaction tx = loanService.issueItem(item.getId(), member.getId(), (int) days);
                showInfoAlert("Success", "Book Issued", "Successfully issued '" + tx.getItemTitle() + "' to " + tx.getMemberName() + ".");
                refreshBookData();
                refreshMemberData();
                refreshLoanData();
            } catch (Exception ex) {
                showErrorAlert("Checkout Failed", ex.getMessage());
            }
        });

        btnProcessReturn.setOnAction(e -> {
            Transaction selected = loanTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("No Selection", "Please select an active loan row to return.");
                return;
            }
            try {
                boolean ok = loanService.returnItem(selected.getId());
                if (ok) {
                    showInfoAlert("Return Completed", "Book Returned", "Item returned and inventory copy count restored.");
                    refreshBookData();
                    refreshMemberData();
                    refreshLoanData();
                }
            } catch (Exception ex) {
                showErrorAlert("Return Error", ex.getMessage());
            }
        });

        btnMarkLost.setOnAction(e -> {
            Transaction selected = loanTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("No Selection", "Please select a loan row to mark lost.");
                return;
            }
            try {
                loanService.markLost(selected.getId());
                showInfoAlert("Item Marked Lost", "Inventory Updated", "Item marked as LOST and total copies decremented.");
                refreshBookData();
                refreshMemberData();
                refreshLoanData();
            } catch (Exception ex) {
                showErrorAlert("Error", ex.getMessage());
            }
        });

        layout.getChildren().addAll(issueCard, returnCard);
        return layout;
    }

    // =========================================================================
    // Tab 4: Overdue & Fines View
    // =========================================================================
    private Node createOverdueTabView() {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16));

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnRefresh = new Button("↻ Refresh Overdue Loans (Prioritized by Fine)");
        btnRefresh.getStyleClass().add("btn-primary");
        toolbar.getChildren().addAll(btnRefresh);

        TableView<Transaction> overdueTable = new TableView<>();
        overdueTable.setPlaceholder(new Label("No overdue loans! All books returned on time."));

        TableColumn<Transaction, Number> colId = new TableColumn<>("Tx #");
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colId.setPrefWidth(55);

        TableColumn<Transaction, String> colBook = new TableColumn<>("Book Title");
        colBook.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemTitle()));
        colBook.setPrefWidth(260);

        TableColumn<Transaction, String> colMember = new TableColumn<>("Borrower");
        colMember.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMemberName()));
        colMember.setPrefWidth(180);

        TableColumn<Transaction, String> colDue = new TableColumn<>("Due Date");
        colDue.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDueDate()));
        colDue.setPrefWidth(110);

        TableColumn<Transaction, Number> colDays = new TableColumn<>("Days Late");
        colDays.setCellValueFactory(c -> new SimpleIntegerProperty((int) c.getValue().getOverdueDays()));
        colDays.setPrefWidth(90);

        TableColumn<Transaction, String> colFine = new TableColumn<>("Accrued Fine (₹)");
        colFine.setCellValueFactory(c -> new SimpleStringProperty("₹ " + String.format("%.2f", c.getValue().getFineAmount())));
        colFine.setPrefWidth(130);

        overdueTable.getColumns().addAll(colId, colBook, colMember, colDue, colDays, colFine);
        VBox.setVgrow(overdueTable, Priority.ALWAYS);

        // Fetch prioritized list using PriorityQueue from LoanService
        ObservableList<Transaction> overdueList = FXCollections.observableArrayList(loanService.getOverdueLoansPrioritizedByFine());
        overdueTable.setItems(overdueList);

        btnRefresh.setOnAction(e -> {
            overdueList.setAll(loanService.getOverdueLoansPrioritizedByFine());
        });

        layout.getChildren().addAll(toolbar, overdueTable);
        return layout;
    }

    // =========================================================================
    // Tab 5: Analytics & Category BarChart
    // =========================================================================
    private Node createAnalyticsTabView() {
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(20));

        HBox topBox = new HBox(12);
        topBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Catalog Holdings Distribution by Category");
        title.getStyleClass().add("card-header");
        Button btnRefreshChart = new Button("↻ Refresh Chart Data");
        btnRefreshChart.getStyleClass().add("btn-primary");
        topBox.getChildren().addAll(title, btnRefreshChart);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Genre / Material Category");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Total Holdings Count");

        // [Concept: JavaFX BarChart UI Component]
        categoryChart = new BarChart<>(xAxis, yAxis);
        categoryChart.setTitle("Inventory Breakdown");
        categoryChart.setLegendVisible(false);
        VBox.setVgrow(categoryChart, Priority.ALWAYS);

        updateChartData();

        btnRefreshChart.setOnAction(e -> updateChartData());

        layout.getChildren().addAll(topBox, categoryChart);
        return layout;
    }

    private void updateChartData() {
        categoryChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Holdings");

        Map<String, Long> distribution = catalogService.getInventoryDistribution();
        for (Map.Entry<String, Long> entry : distribution.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        categoryChart.getData().add(series);
    }

    // =========================================================================
    // Dialogs with Form Validation (Unit 5 requirement)
    // =========================================================================
    private void showAddHoldingDialog() {
        Dialog<LibraryItem> dialog = new Dialog<>();
        dialog.setTitle("Add New Library Holding");
        dialog.setHeaderText("Enter details to catalog a new Book, Magazine, Journal, or Reference item.");

        ButtonType saveButtonType = new ButtonType("Add to Catalog", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<ItemType> comboType = new ComboBox<>(FXCollections.observableArrayList(ItemType.values()));
        comboType.setValue(ItemType.BOOK);

        TextField titleField = new TextField();
        TextField publisherField = new TextField();
        TextField yearField = new TextField(String.valueOf(LocalDate.now().getYear()));
        TextField copiesField = new TextField("3");

        // Book specific
        TextField authorField = new TextField();
        TextField isbnField = new TextField();
        TextField genreField = new TextField("Computer Science");

        grid.add(new Label("Type:"), 0, 0);
        grid.add(comboType, 1, 0);
        grid.add(new Label("Title:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Publisher:"), 0, 2);
        grid.add(publisherField, 1, 2);
        grid.add(new Label("Year:"), 0, 3);
        grid.add(yearField, 1, 3);
        grid.add(new Label("Copies:"), 0, 4);
        grid.add(copiesField, 1, 4);
        grid.add(new Label("Author:"), 0, 5);
        grid.add(authorField, 1, 5);
        grid.add(new Label("ISBN:"), 0, 6);
        grid.add(isbnField, 1, 6);
        grid.add(new Label("Genre:"), 0, 7);
        grid.add(genreField, 1, 7);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String title = titleField.getText().trim();
                    String publisher = publisherField.getText().trim();
                    int year = Integer.parseInt(yearField.getText().trim());
                    int copies = Integer.parseInt(copiesField.getText().trim());

                    if (title.isEmpty() || publisher.isEmpty()) {
                        throw new IllegalArgumentException("Title and Publisher cannot be empty.");
                    }

                    ItemType type = comboType.getValue();
                    if (type == ItemType.BOOK) {
                        String isbn = isbnField.getText().trim();
                        ValidationUtil.validateISBN(isbn); // [Concept: Defensive validation]
                        return new Book(0, title, publisher, year, copies, copies, isbn, authorField.getText().trim(), genreField.getText().trim());
                    } else if (type == ItemType.MAGAZINE) {
                        return new Magazine(0, title, publisher, year, copies, copies, 1, "Current", "General");
                    } else if (type == ItemType.JOURNAL) {
                        return new Journal(0, title, publisher, year, copies, copies, "Vol. 1", true, 2.5);
                    } else {
                        return new ReferenceBook(0, title, publisher, year, copies, copies, "General", "1st Edition", "000");
                    }
                } catch (Exception ex) {
                    showErrorAlert("Validation Error", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<LibraryItem> result = dialog.showAndWait();
        result.ifPresent(item -> {
            try {
                catalogService.addItem(item);
                refreshBookData();
                updateChartData();
                showInfoAlert("Success", "Holding Added", "'" + item.getTitle() + "' was successfully cataloged.");
            } catch (Exception ex) {
                showErrorAlert("Cataloging Error", ex.getMessage());
            }
        });
    }

    private void showAddMemberDialog() {
        Dialog<Member> dialog = new Dialog<>();
        dialog.setTitle("Register New Library Member");
        dialog.setHeaderText("Enter patron profile and borrowing quota details.");

        ButtonType saveBtn = new ButtonType("Register Patron", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 120, 10, 10));

        TextField nameField = new TextField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField("+91 ");
        TextField limitField = new TextField("3");

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Max Borrow Limit:"), 0, 3);
        grid.add(limitField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    String name = nameField.getText().trim();
                    String email = emailField.getText().trim();
                    String phone = phoneField.getText().trim();
                    int limit = Integer.parseInt(limitField.getText().trim());

                    if (!ValidationUtil.isValidEmail(email)) {
                        throw new IllegalArgumentException("Invalid email format: " + email);
                    }
                    String memId = "MEM-" + (1000 + (int)(Math.random() * 9000));
                    String defaultHash = com.library.util.PasswordUtil.hash("member123");
                    return new Member(name, email, phone, defaultHash, memId, limit);
                } catch (Exception ex) {
                    showErrorAlert("Input Validation Error", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Member> result = dialog.showAndWait();
        result.ifPresent(m -> {
            try {
                memberDAO.save(m);
                refreshMemberData();
                showInfoAlert("Success", "Patron Registered", "Member '" + m.getName() + "' created with ID: " + m.getMembershipId());
            } catch (Exception ex) {
                showErrorAlert("Registration Failed", ex.getMessage());
            }
        });
    }

    private void deleteSelectedBook() {
        LibraryItem selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("No Selection", "Please click on a book in the table first.");
            return;
        }

        // [Concept: Alert Dialog for Confirmation]
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete '" + selected.getTitle() + "'?");
        confirm.setContentText("Are you sure you want to remove this item from the catalog? This action cannot be undone.");

        Optional<ButtonType> btn = confirm.showAndWait();
        if (btn.isPresent() && btn.get() == ButtonType.OK) {
            try {
                catalogService.deleteItem(selected.getId());
                refreshBookData();
                updateChartData();
                showInfoAlert("Deleted", "Item Removed", "The holding has been removed from catalog.");
            } catch (Exception ex) {
                showErrorAlert("Deletion Failed", ex.getMessage());
            }
        }
    }

    // Refresh Helpers
    private void refreshBookData() {
        masterBookList.setAll(catalogService.getAllItems("title"));
    }

    private void refreshMemberData() {
        masterMemberList.setAll(memberDAO.findAllMembers());
    }

    private void refreshLoanData() {
        masterLoanList.setAll(loanService.getActiveLoans());
    }

    // Helper alerts
    private void showInfoAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("An error occurred");
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
