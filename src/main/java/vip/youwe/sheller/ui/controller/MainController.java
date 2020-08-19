package vip.youwe.sheller.ui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.json.JSONArray;
import org.json.JSONObject;
import vip.youwe.sheller.core.Constants;
import vip.youwe.sheller.dao.ShellManager;
import vip.youwe.sheller.utils.Utils;

import java.io.ByteArrayInputStream;
import java.net.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Allen
 * @Description:
 * @Date: 2020/08/18 11:36:51
 * @Version 1.0
 **/
public class MainController {
    @FXML
    private TreeView treeview;
    @FXML
    private TableView shellListTable;
    @FXML
    private TableColumn urlCol;
    @FXML
    private TableColumn ipCol;
    @FXML
    private TableColumn typeCol;
    @FXML
    private TableColumn osCol;
    @FXML
    private TableColumn commentCol;
    public static Map<String, Object> currentProxy = new HashMap();
    @FXML
    private TableColumn addTimeCol;
    @FXML
    private MenuItem proxySetupBtn;
    @FXML
    private Label statusLabel;

    public MainController() {
        try {
            this.shellManager = new ShellManager();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            showErrorMessage("错误", "数据库文件丢失");
            System.exit(0);
        }
    }

    @FXML
    private Label versionLabel;
    @FXML
    private Label proxyStatusLabel;
    @FXML
    private TreeView catagoryTreeView;
    private ShellManager shellManager;

    public void initialize() {
        try {
            initCatagoryList();
            initShellList();
            initToolbar();
            initBottomBar();
            loadProxy();
        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    private void initBottomBar() {
        this.versionLabel.setText(String.format(this.versionLabel.getText(), Constants.VERSION));
    }


    private void loadProxy() throws Exception {
        JSONObject proxyObj = this.shellManager.findProxy("default");
        int status = proxyObj.getInt("status");
        String type = proxyObj.getString("type");
        String ip = proxyObj.getString("ip");
        String port = proxyObj.get("port").toString();
        String username = proxyObj.getString("username");
        String password = proxyObj.getString("password");
        if (status == Constants.PROXY_ENABLE) {

            currentProxy.put("username", username);
            currentProxy.put("password", password);
            InetSocketAddress proxyAddr = new InetSocketAddress(ip, Integer.parseInt(port));
            if (type.equals("HTTP")) {

                Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
                currentProxy.put("proxy", proxy);
            } else if (type.equals("SOCKS")) {

                Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);
                currentProxy.put("proxy", proxy);
            }
            this.proxyStatusLabel.setText("代理生效中");
        }
    }

    private void initToolbar() {
        this.proxySetupBtn.setOnAction(event -> {
            Alert inputDialog = new Alert(Alert.AlertType.NONE);
            Window window = inputDialog.getDialogPane().getScene().getWindow();
            window.setOnCloseRequest(e-> window.hide());
            ToggleGroup statusGroup = new ToggleGroup();
            RadioButton enableRadio = new RadioButton("启用");
            RadioButton disableRadio = new RadioButton("禁用");
            enableRadio.setToggleGroup(statusGroup);
            disableRadio.setToggleGroup(statusGroup);
            HBox statusHbox = new HBox();
            statusHbox.setSpacing(10.0D);
            statusHbox.getChildren().add(enableRadio);
            statusHbox.getChildren().add(disableRadio);
            GridPane proxyGridPane = new GridPane();
            proxyGridPane.setVgap(15.0D);
            proxyGridPane.setPadding(new Insets(20.0D, 20.0D, 0.0D, 10.0D));
            Label typeLabel = new Label("类型：");
            ComboBox typeCombo = new ComboBox();
            typeCombo.setItems(FXCollections.observableArrayList("HTTP", "SOCKS"));
            typeCombo.getSelectionModel().select(0);
            Label IPLabel = new Label("IP地址：");
            TextField IPText = new TextField();
            Label PortLabel = new Label("端口：");
            TextField PortText = new TextField();
            Label userNameLabel = new Label("用户名：");
            TextField userNameText = new TextField();
            Label passwordLabel = new Label("密码：");
            TextField passwordText = new TextField();
            Button cancelBtn = new Button("取消");
            Button saveBtn = new Button("保存");
            try {
                JSONObject proxyObj = this.shellManager.findProxy("default");
                if (proxyObj != null) {
                    int status = proxyObj.getInt("status");
                    if (status == Constants.PROXY_ENABLE) {

                        enableRadio.setSelected(true);
                    } else if (status == Constants.PROXY_DISABLE) {

                        disableRadio.setSelected(true);
                    }
                    String type = proxyObj.getString("type");
                    if (type.equals("HTTP")) {

                        typeCombo.getSelectionModel().select(0);
                    } else if (type.equals("SOCKS")) {

                        typeCombo.getSelectionModel().select(1);
                    }
                    String ip = proxyObj.getString("ip");
                    String port = proxyObj.get("port").toString();
                    IPText.setText(ip);
                    PortText.setText(port);
                    String username = proxyObj.getString("username");
                    String password = proxyObj.getString("password");
                    userNameText.setText(username);
                    passwordText.setText(password);
                }

            } catch (Exception e) {
                this.statusLabel.setText("代理服务器配置加载失败。");
                e.printStackTrace();
            }
            saveBtn.setOnAction((e) -> {
                if (disableRadio.isSelected()) {
                    currentProxy.put("proxy", null);
                    this.proxyStatusLabel.setText("");

                    try {
                        this.shellManager.updateProxy("default", typeCombo.getSelectionModel().getSelectedItem().toString(), IPText.getText(), PortText.getText(), userNameText.getText(), passwordText.getText(), Constants.PROXY_DISABLE);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                    inputDialog.getDialogPane().getScene().getWindow().hide();
                } else {
                    try {
                        this.shellManager.updateProxy("default", typeCombo.getSelectionModel().getSelectedItem().toString(), IPText.getText(), PortText.getText(), userNameText.getText(), passwordText.getText(), Constants.PROXY_ENABLE);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                    final String type;
                    // String type;
                    if (!userNameText.getText().trim().equals("")) {
                        final String proxyUser = userNameText.getText().trim();
                        // String proxyUser = userNameText.getText().trim();
                        type = passwordText.getText();
                        Authenticator.setDefault(new Authenticator() {
                            public PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(proxyUser, type.toCharArray());
                            }
                        });
                    } else {
                        Authenticator.setDefault(null);
                    }

                    currentProxy.put("username", userNameText.getText());
                    currentProxy.put("password", passwordText.getText());
                    InetSocketAddress proxyAddr = new InetSocketAddress(IPText.getText(), Integer.parseInt(PortText.getText()));
                    String pType = typeCombo.getValue().toString();
                    Proxy proxy;
                    if (pType.equals("HTTP")) {
                        proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
                        currentProxy.put("proxy", proxy);
                    } else if (pType.equals("SOCKS")) {
                        proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);
                        currentProxy.put("proxy", proxy);
                    }

                    this.proxyStatusLabel.setText("代理生效中");
                    inputDialog.getDialogPane().getScene().getWindow().hide();
                }
            });

            cancelBtn.setOnAction((e) -> inputDialog.getDialogPane().getScene().getWindow().hide());

            proxyGridPane.add(statusHbox, 1, 0);
            proxyGridPane.add(typeLabel, 0, 1);
            proxyGridPane.add(typeCombo, 1, 1);
            proxyGridPane.add(IPLabel, 0, 2);
            proxyGridPane.add(IPText, 1, 2);
            proxyGridPane.add(PortLabel, 0, 3);
            proxyGridPane.add(PortText, 1, 3);
            proxyGridPane.add(userNameLabel, 0, 4);
            proxyGridPane.add(userNameText, 1, 4);
            proxyGridPane.add(passwordLabel, 0, 5);
            proxyGridPane.add(passwordText, 1, 5);
            HBox buttonBox = new HBox();
            buttonBox.setSpacing(20.0D);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.getChildren().add(cancelBtn);
            buttonBox.getChildren().add(saveBtn);
            GridPane.setColumnSpan(buttonBox, 2);
            proxyGridPane.add(buttonBox, 0, 6);
            inputDialog.getDialogPane().setContent(proxyGridPane);
            inputDialog.showAndWait();
        });
    }

    private void initCatagoryList() throws Exception {
        initCatagoryTree();
        initCatagoryMenu();
    }

    private void initShellList() throws Exception {
        initShellTable();
        loadShellList();
        loadContextMenu();
    }

    private void initShellTable() {
        ObservableList<TableColumn<List<StringProperty>, ?>> tcs = this.shellListTable.getColumns();
        for (int i = 0; i < tcs.size(); i++) {
            int j = i;
            tcs.get(i).setCellValueFactory(data -> (ObservableValue) data.getValue().get(j));
            // ((TableColumn) tcs.get(i)).setCellValueFactory(data -> ((List) data.getValue()).get(j));
        }
        this.shellListTable.setRowFactory((tv) -> {
            TableRow<List<StringProperty>> row = new TableRow();
            row.setOnMouseClicked((event) -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    String url = ((StringProperty) ((List) row.getItem()).get(0)).getValue();
                    String shellID = ((StringProperty) ((List) row.getItem()).get(6)).getValue();
                    this.openShell(url, shellID);
                }

            });
            return row;
        });
    }


    private boolean checkUrl(String urlString) {
        try {
            URL url = new URL(urlString.trim());
            return true;
        } catch (Exception e) {
            showErrorMessage("错误", "URL格式错误");
            return false;
        }
    }


    private boolean checkPassword(String password) {
        if (password.length() > 255) {
            showErrorMessage("错误", "密码长度不应大于255个字符");
            return false;
        }
        if (password.length() < 1) {

            showErrorMessage("错误", "密码不能为空，请输入密码");
            return false;
        }
        return true;
    }

    private void showShellDialog(int shellID) throws Exception {
        Alert alert = new Alert(Alert.AlertType.NONE);

        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(e -> window.hide());

        alert.setTitle("新增Shell");
        alert.setHeaderText("");
        TextField urlText = new TextField();


        TextField passText = new TextField();
        ComboBox shellType = new ComboBox();
        ObservableList<String> typeList = FXCollections.observableArrayList("jsp", "php", "aspx", "asp");
        shellType.setItems(typeList);

        ComboBox shellCatagory = new ComboBox();
        try {
            JSONArray catagoryArr = this.shellManager.listCatagory();
            ObservableList<String> catagoryList = FXCollections.observableArrayList();
            for (int i = 0; i < catagoryArr.length(); i++) {

                JSONObject catagoryObj = catagoryArr.getJSONObject(i);
                catagoryList.add(catagoryObj.getString("name"));
            }
            shellCatagory.setItems(catagoryList);
            shellCatagory.getSelectionModel().select(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TextArea header = new TextArea();
        TextArea commnet = new TextArea();
        urlText.textProperty().addListener((observable, oldValue, newValue) -> {
            URL url;
            try {
                url = new URL(urlText.getText().trim());
            } catch (Exception e) {
                return;
            }
            String extension = url.getPath().substring(url.getPath().lastIndexOf(".") + 1).toLowerCase();

            for (int i = 0; i < shellType.getItems().size(); i++) {

                if (extension.toLowerCase().equals(shellType.getItems().get(i))) {
                    shellType.getSelectionModel().select(i);
                }
            }
        });


        Button saveBtn = new Button("保存");
        Button cancelBtn = new Button("取消");

        GridPane vpsInfoPane = new GridPane();
        GridPane.setMargin(vpsInfoPane, new Insets(20.0D, 0.0D, 0.0D, 0.0D));
        vpsInfoPane.setVgap(10.0D);
        vpsInfoPane.setMaxWidth(Double.MAX_VALUE);
        vpsInfoPane.add(new Label("URL："), 0, 0);
        vpsInfoPane.add(urlText, 1, 0);


        vpsInfoPane.add(new Label("密码："), 0, 1);
        vpsInfoPane.add(passText, 1, 1);
        vpsInfoPane.add(new Label("脚本类型："), 0, 2);
        vpsInfoPane.add(shellType, 1, 2);
        vpsInfoPane.add(new Label("分类："), 0, 3);
        vpsInfoPane.add(shellCatagory, 1, 3);
        vpsInfoPane.add(new Label("自定义请求头："), 0, 4);
        vpsInfoPane.add(header, 1, 4);
        vpsInfoPane.add(new Label("备注："), 0, 5);
        vpsInfoPane.add(commnet, 1, 5);
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(20.0D);
        buttonBox.getChildren().addAll(cancelBtn, saveBtn);
        buttonBox.setAlignment(Pos.BOTTOM_CENTER);
        vpsInfoPane.add(buttonBox, 0, 8);
        GridPane.setColumnSpan(buttonBox, 2);
        alert.getDialogPane().setContent(vpsInfoPane);

        if (shellID != -1) {

            JSONObject shellObj = this.shellManager.findShell(shellID);
            urlText.setText(shellObj.getString("url"));
            passText.setText(shellObj.getString("password"));
            shellType.setValue(shellObj.getString("type"));
            shellCatagory.setValue(shellObj.getString("catagory"));
            header.setText(shellObj.getString("headers"));
            commnet.setText(shellObj.getString("comment"));
        }
        saveBtn.setOnAction(e -> {
            String url = urlText.getText().trim();
            String password = passText.getText();

            if (!checkUrl(url) || !checkPassword(password)) {
                return;
            }

            String type = shellType.getValue().toString();
            String catagory = shellCatagory.getValue().toString();
            String comment = commnet.getText();
            String headers = header.getText();
            try {
                if (shellID == -1) {
                    this.shellManager.addShell(url, password, type, catagory, comment, headers);
                } else {
                    this.shellManager.updateShell(shellID, url, password, type, catagory, comment, headers);
                }
                loadShellList();
            } catch (Exception e1) {

                e1.printStackTrace();
                showErrorMessage("保存失败", e1.getMessage());

                return;
            } finally {
                alert.getDialogPane().getScene().getWindow().hide();
            }
        });
        cancelBtn.setOnAction(e ->
                alert.getDialogPane().getScene().getWindow().hide());

        alert.showAndWait();
    }


    private void openShell(String url, String shellID) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vip/youwe/sheller/ui/MainWindow.fxml"));
            Parent mainWindow = loader.load();
            MainWindowController mainWindowController = loader.getController();

            mainWindowController.init(this.shellManager.findShell(Integer.parseInt(shellID)), this.shellManager, currentProxy);
            Stage stage = new Stage();
            stage.setTitle(url);
            stage.getIcons().add(new Image(new ByteArrayInputStream(Utils.getResourceData("logo.jpg"))));
            stage.setUserData(url);
            stage.setScene(new Scene(mainWindow));
            stage.setOnCloseRequest(e -> {
                for (Thread worker : mainWindowController.getWorkList()) {
                    worker.interrupt();
                }
            });
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadContextMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem openBtn = new MenuItem("打开");
        cm.getItems().add(openBtn);
        MenuItem addBtn = new MenuItem("新增");
        cm.getItems().add(addBtn);
        MenuItem editBtn = new MenuItem("编辑");
        cm.getItems().add(editBtn);
        MenuItem delBtn = new MenuItem("删除");
        cm.getItems().add(delBtn);
        MenuItem copyBtn = new MenuItem("复制URL");
        cm.getItems().add(copyBtn);
        SeparatorMenuItem separatorBtn = new SeparatorMenuItem();
        cm.getItems().add(separatorBtn);
        MenuItem refreshBtn = new MenuItem("刷新");
        cm.getItems().add(refreshBtn);
        this.shellListTable.setContextMenu(cm);
        openBtn.setOnAction(event -> {

            String url = ((StringProperty) ((List) this.shellListTable.getSelectionModel().getSelectedItem()).get(0)).getValue();
            String shellID = ((StringProperty) ((List) this.shellListTable.getSelectionModel().getSelectedItem()).get(6)).getValue();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/vip/youwe/sheller/ui/MainWindow.fxml"));
                // FXMLLoader loader = new FXMLLoader(getClass().getResource("../MainWindow.fxml"));
                Parent mainWindow = loader.load();
                MainWindowController mainWindowController = loader.getController();

                mainWindowController.init(this.shellManager.findShell(Integer.parseInt(shellID)), this.shellManager, currentProxy);
                Stage stage = new Stage();
                stage.setTitle(url);
                stage.getIcons().add(new Image(new ByteArrayInputStream(Utils.getResourceData("logo.jpg"))));
                stage.setUserData(url);
                stage.setScene(new Scene(mainWindow));
                stage.setOnCloseRequest(e -> {
                    for (Thread worker : mainWindowController.getWorkList()) {
                        worker.interrupt();
                    }
                });

                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        addBtn.setOnAction(event -> {
            try {
                showShellDialog(-1);
            } catch (Exception e) {
                showErrorMessage("错误", "新增失败：" + e.getMessage());
                e.printStackTrace();
            }
        });
        editBtn.setOnAction(event -> {
            String shellID = ((StringProperty) ((List) this.shellListTable.getSelectionModel().getSelectedItem()).get(6)).getValue();
            try {
                showShellDialog(Integer.parseInt(shellID));
            } catch (Exception e) {
                showErrorMessage("错误", "编辑失败：" + e.getMessage());
                e.printStackTrace();
            }
        });

        delBtn.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("");
            alert.setContentText("请确认是否删除？");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                String shellID = ((StringProperty) ((List) this.shellListTable.getSelectionModel().getSelectedItem()).get(6)).getValue();
                try {
                    this.shellManager.deleteShell(Integer.parseInt(shellID));
                    loadShellList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        copyBtn.setOnAction(event -> {
            String url = ((StringProperty) ((List) this.shellListTable.getSelectionModel().getSelectedItem()).get(0)).getValue();
            copyString(url);
        });
        refreshBtn.setOnAction(event -> {
            try {
                loadShellList();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadShellList() throws Exception {
        this.shellListTable.getItems().clear();
        JSONArray shellList = this.shellManager.listShell();
        fillShellRows(shellList);
    }

    private void fillShellRows(JSONArray jsonArray) {
        ObservableList<List<StringProperty>> data = FXCollections.observableArrayList();

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject rowObj = jsonArray.getJSONObject(i);
            try {
                int id = rowObj.getInt("id");
                String url = rowObj.getString("url");
                String ip = rowObj.getString("ip");
                String type = rowObj.getString("type");
                String os = rowObj.getString("os");
                String comment = rowObj.getString("comment");
                SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String addTime = df.format(new Timestamp(rowObj.getLong("addtime")));

                List<StringProperty> row = new ArrayList<StringProperty>();
                row.add(0, new SimpleStringProperty(url));
                row.add(1, new SimpleStringProperty(ip));
                row.add(2, new SimpleStringProperty(type));
                row.add(3, new SimpleStringProperty(os));
                row.add(4, new SimpleStringProperty(comment));
                row.add(5, new SimpleStringProperty(addTime));
                row.add(6, new SimpleStringProperty(id + ""));
                data.add(row);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.shellListTable.setItems(data);
    }


    private void copyString(String str) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(str);
        clipboard.setContent(content);
    }

    private void showErrorMessage(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> window.hide());

        alert.setTitle(title);
        alert.setHeaderText("");
        alert.setContentText(msg);
        alert.show();
    }


    private void initCatagoryMenu() {
        ContextMenu treeContextMenu = new ContextMenu();
        MenuItem addCatagoryBtn = new MenuItem("新增");
        treeContextMenu.getItems().add(addCatagoryBtn);
        MenuItem delCatagoryBtn = new MenuItem("删除");
        treeContextMenu.getItems().add(delCatagoryBtn);

        addCatagoryBtn.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("新增分类");
            alert.setHeaderText("");
            GridPane panel = new GridPane();
            Label cataGoryNameLable = new Label("请输入分类名称：");
            TextField cataGoryNameTxt = new TextField();
            Label cataGoryCommentLable = new Label("请输入分类描述：");
            TextField cataGoryCommentTxt = new TextField();
            panel.add(cataGoryNameLable, 0, 0);
            panel.add(cataGoryNameTxt, 1, 0);
            panel.add(cataGoryCommentLable, 0, 1);
            panel.add(cataGoryCommentTxt, 1, 1);
            panel.setVgap(20.0D);
            alert.getDialogPane().setContent(panel);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                try {
                    if (this.shellManager.addCatagory(cataGoryNameTxt.getText(), cataGoryCommentTxt.getText()) > 0) {

                        this.statusLabel.setText("分类新增完成");
                        initCatagoryTree();
                    }
                } catch (Exception e) {
                    this.statusLabel.setText("分类新增失败：" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        delCatagoryBtn.setOnAction(event -> {
            if (this.catagoryTreeView.getSelectionModel().getSelectedItem() == null)
                return;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("");
            alert.setContentText("请确认是否删除？仅删除分类信息，不会删除该分类下的网站。");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {

                try {
                    String cataGoryName = ((TreeItem) this.catagoryTreeView.getSelectionModel().getSelectedItem()).getValue().toString();

                    if (this.shellManager.deleteCatagory(cataGoryName) > 0) {

                        this.statusLabel.setText("分类删除完成");
                        initCatagoryTree();
                    }
                } catch (Exception e) {
                    this.statusLabel.setText("分类删除失败：" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        this.catagoryTreeView.setContextMenu(treeContextMenu);
        this.catagoryTreeView.setOnMouseClicked(event -> {
            TreeItem currentTreeItem = (TreeItem) this.catagoryTreeView.getSelectionModel().getSelectedItem();
            if (currentTreeItem.isLeaf()) {

                String catagoryName = currentTreeItem.getValue().toString();
                try {
                    this.shellListTable.getItems().clear();
                    JSONArray shellList = this.shellManager.findShellByCatagory(catagoryName);
                    fillShellRows(shellList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {


                try {
                    this.shellListTable.getItems().clear();
                    loadShellList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initCatagoryTree() throws Exception {
        JSONArray catagoryList = this.shellManager.listCatagory();
        TreeItem<String> rootItem = new TreeItem<String>("分类列表", new ImageView());
        for (int i = 0; i < catagoryList.length(); i++) {

            JSONObject catagoryObj = catagoryList.getJSONObject(i);
            TreeItem<String> treeItem = new TreeItem<String>(catagoryObj.getString("name"));
            rootItem.getChildren().add(treeItem);
        }
        rootItem.setExpanded(true);
        this.catagoryTreeView.setRoot(rootItem);
        this.catagoryTreeView.getSelectionModel().select(rootItem);
    }
}
