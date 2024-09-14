package ch.epfl.biop.wrappers.cellpose.ij2commands;

import ch.epfl.biop.wrappers.BiopWrappersCheck;
import ch.epfl.biop.wrappers.cellpose.Cellpose;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

/**
 * Checks whether the executable being wrapped are accessible
 */

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Cellpose>Cellpose setup...")
public class CellposePrefsSet implements Command {

    @Parameter
    LogService ls;

    @Parameter(required = true, style = "directory", persist = false)
    File cellposeEnvDirectory = new File(Cellpose.envDirPath);

    @Parameter(required = true, choices = {"conda", "venv"}, persist = false)
    String envType = Cellpose.envType;

    @Parameter(persist = false)
    Boolean useGpu = Cellpose.useGpu;

    @Parameter(persist = false)
    Boolean useMxnet = Cellpose.useMxnet;

    @Parameter(persist = false)
    Boolean useFastMode = Cellpose.useFastMode;

    @Parameter(persist = false)
    Boolean useResample = Cellpose.useResample;

    @Parameter(choices = {"0.6", "0.7", "1.0", "2.0"， "3.0"}, persist = false)
    String version = Cellpose.version;

    @Override
    public void run() {

        Cellpose.setEnvDirPath(cellposeEnvDirectory);
        Cellpose.setEnvType(envType);

        Cellpose.setUseGpu(useGpu);
        Cellpose.setUseMxnet(useMxnet);
        Cellpose.setUseFastMode(useFastMode);
        Cellpose.setUseResample(useResample);
        Cellpose.setVersion(version);

        if (ls != null) {
            ls.info(BiopWrappersCheck.isCellposeSet());
        }
    }
}
