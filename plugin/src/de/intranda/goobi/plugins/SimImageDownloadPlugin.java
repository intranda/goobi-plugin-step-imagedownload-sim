package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import io.goobi.workflow.api.connection.HttpUtils;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;

@PluginImplementation
public class SimImageDownloadPlugin implements IStepPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(SimImageDownloadPlugin.class);

    private static final String PLUGIN_NAME = "SimImageDownload";

    private Step step;
    private String returnPath;

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String getTitle() {
        return PLUGIN_NAME;
    }

    public String getDescription() {
        return PLUGIN_NAME;
    }

    @Override
    public void initialize(Step step, String returnPath) {
        this.step = step;
        this.returnPath = returnPath;
    }

    @Override
    public String cancel() {
        return returnPath;
    }

    @Override
    public String finish() {
        return returnPath;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public Step getStep() {
        return step;
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return null;
    }

    @Override
    public boolean execute() {
        Process process = step.getProzess();

        try {
            Fileformat ff = process.readMetadataFile();
            DocStruct log = ff.getDigitalDocument().getLogicalDocStruct();
            String downloadFolder = process.getImagesOrigDirectory(false);
            List<? extends Metadata> metadataList =
                    log.getAllMetadataByType(process.getRegelsatz().getPreferences().getMetadataTypeByName("_imageName"));

            if (metadataList != null && !metadataList.isEmpty()) {
                for (Metadata md : metadataList) {
                    String url = md.getValue().trim();
                    if (url.startsWith("http://www.gbv.de/dms/sim-prog/")) {
                        downloadFile(url, downloadFolder);
                    }
                }
            }

        } catch (ReadException | PreferencesException | SwapException | DAOException | IOException e) {
            logger.error(e);
            Helper.addMessageToProcessJournal(process.getId(), LogType.ERROR, e.getMessage(), "automatic");
            return false;
        }
        Helper.addMessageToProcessJournal(process.getId(), LogType.INFO, "Download der Bilder abgeschlossen.", "automatic");
        return true;
    }

    private void downloadFile(String url, String downloadFolder) throws IOException {
        String[] parts = url.split("/");
        String imgName = parts[parts.length - 1];
        if (imgName.contains(".")) {
            imgName = imgName.substring(0, imgName.indexOf(".")) + imgName.substring(imgName.lastIndexOf("."));
        }

        File imageFile = new File(downloadFolder, imgName);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        try (OutputStream ostr = new FileOutputStream(imageFile)) {
            HttpUtils.getStreamFromUrl(ostr, url);

        }

    }
}
