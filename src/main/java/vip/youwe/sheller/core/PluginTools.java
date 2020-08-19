package vip.youwe.sheller.core;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import org.json.JSONObject;
import vip.youwe.sheller.utils.Utils;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginTools {

    private ShellService currentShellService;
    private Label statusLabel;
    private WebView pluginWebview;
    private JSONObject shellEntity;
    private Map<String, String> taskMap = new HashMap();

    private List<Thread> workList;

    public PluginTools(ShellService shellService, WebView pluginWebview, Label statusLabel, List<Thread> workList) {
        this.currentShellService = shellService;
        this.shellEntity = shellService.shellEntity;
        this.workList = workList;
        this.pluginWebview = pluginWebview;
        this.statusLabel = statusLabel;
    }


    public PluginTools(ShellService shellService, Label statusLabel, List<Thread> workList) {
        this.currentShellService = shellService;
        this.shellEntity = shellService.shellEntity;
        this.workList = workList;
        this.statusLabel = statusLabel;
    }


    public void sendTask(String pluginName, String paramStr) throws Exception {
        String type = this.shellEntity.getString("type");
        if (type.equals("jsp")) type = "java";

        String payloadPath = String.format("/Users/rebeyond/Documents/Behinder/plugin/%s/payload/%s.payload", pluginName, type);
        JSONObject paramObj = new JSONObject(paramStr);
        Map<String, String> params = Utils.jsonToMap(paramObj);

        String taskID = pluginName;
        params.put("taskID", taskID);
        this.statusLabel.setText("正在执行插件……");
        Runnable runner = () -> {
            try {
                JSONObject resultObj = this.currentShellService.submitPluginTask(taskID, payloadPath, params);
                String status = resultObj.getString("status");
                String msg = resultObj.getString("msg");
                Platform.runLater(() -> this.statusLabel.setText(msg));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> this.statusLabel.setText("插件运行失败"));
            }
        };


        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }

    public void sendTaskBackground(String pluginName, Map<String, String> params, PluginSubmitCallBack callBack) throws Exception {
        String type = this.shellEntity.getString("type");
        if (type.equals("jsp")) type = "java";

        String payloadPath = String.format("/Users/rebeyond/Documents/Behinder/plugin/%s/payload/%s.payload", pluginName, type);

        String taskID = pluginName;
        params.put("taskID", taskID);
        Runnable runner = () -> {
            try {
                JSONObject resultObj = this.currentShellService.submitPluginTask(taskID, payloadPath, params);
                String status = resultObj.getString("status");
                String msg = resultObj.getString("msg");
                callBack.onPluginSubmit(status, msg);
            } catch (Exception e) {
                e.printStackTrace();
                callBack.onPluginSubmit("fail", e.getMessage());
            }
        };
        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }


    public String queryTaskList() {
        return "";
    }


    public String queryTask(String taskName) {
        return "";
    }


    public void getTaskResult(String pluginName) {
        this.statusLabel.setText("正在刷新任务执行结果……");
        Runnable runner = () -> {
            try {
                JSONObject resultObj = this.currentShellService.getPluginTaskResult(pluginName);
                String status = resultObj.getString("status");

                String msg = resultObj.getString("msg");
                JSONObject msgObj = new JSONObject(msg);

                String pluginResult = new String(Base64.getDecoder().decode(msgObj.getString("result")), "UTF-8");
                String pluginRunning = msgObj.getString("running");
                Platform.runLater(() -> {
                    if (status.equals("success")) {
                        this.statusLabel.setText("结果刷新成功");

                        try {
                            this.pluginWebview.getEngine().executeScript(
                                    String.format("onResult('%s','%s','%s')", status, pluginResult, pluginRunning));
                        } catch (Exception e) {
                            this.statusLabel.setText("结果刷新成功，但是插件解析结果失败，请检查插件:" + e.getMessage());
                        }
                    } else {
                        this.statusLabel.setText("结果刷新失败");
                    }

                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> this.statusLabel.setText("结果刷新失败:" + e.getMessage()));
            }
        };


        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }


    public void getTaskResultBackground(String pluginName, PluginResultCallBack callBack) {
        Runnable runner = () -> {
            String running = "true";
            try {
                while (running.equals("true")) {
                    JSONObject resultObj = this.currentShellService.getPluginTaskResult(pluginName);
                    String status = resultObj.getString("status");

                    String msg = resultObj.getString("msg");
                    JSONObject msgObj = new JSONObject(msg);
                    String pluginResult = new String(Base64.getDecoder().decode(msgObj.getString("result")), "UTF-8");
                    String pluginRunning = msgObj.getString("running");
                    running = pluginRunning;
                    callBack.onPluginResult(status, pluginResult, pluginRunning);
                    Thread.sleep(3000L);
                }

            } catch (Exception e) {
                callBack.onPluginResult("fail", e.getMessage(), "false");
            }
        };
        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }
}
