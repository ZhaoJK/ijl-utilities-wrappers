package ch.epfl.biop.wrappers.cellpose.ij2commands;

import ch.epfl.biop.wrappers.cellpose.CellposeTaskSettings;
import ch.epfl.biop.wrappers.cellpose.DefaultCellposeTask;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import net.imagej.ImageJ;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Cellpose> Cellpose Advanced (custom model)")
public class Cellpose_SegmentImgPlusOwnModelAdvanced implements Command {

    @Parameter
    ImagePlus imp;

    // value defined from https://cellpose.readthedocs.io/en/latest/api.html
    @Parameter
    int diameter = 30;

    @Parameter
    double cellproba_threshold = 0.0;

    @Parameter
    double flow_threshold = 0.4;

    @Parameter
    double anisotropy = 1.0;

    @Parameter(required = false)
    File model_path = new File("cellpose");

    @Parameter(choices = {"nuclei",
                            "cyto",
                            "cyto2",
                            "cyto2_omni",
                            "bact_omni",
                            "own model nuclei",
                            "own model cyto",
                            "own model cyto2",
                            "own model cyto2_ommi",
                            "own model bact_omni"}, callback = "modelchanged")
    String model;

    @Parameter(label = "Set to 0 if not necessary (for cyto and bact models)")
    int nuclei_channel;

    @Parameter
    int cyto_channel;

    @Parameter(choices = {"2D", "3D"})
    String dimensionMode;

    @Parameter
    double stitch_threshold;

    @Parameter
    boolean omni;

    @Parameter
    boolean cluster;

    @Parameter(required = false)
    String additionnal_flags;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus cellpose_imp;

    Boolean verbose = true;


    // propose some default value when a model is selected
    public void modelchanged() {
        if (model.equals("nuclei")) {
            nuclei_channel = 1;
            cyto_channel = -1;
        } else if ((model.equals("cyto")) || (model.equals("cyto2")) || (model.equals("cyto2_omni"))) {
            cyto_channel = 1;
            nuclei_channel = 2;
        } else if ((model.equals("bact_omni"))) {
            cyto_channel = 1;
            nuclei_channel = -1;
        } else if (model.equals("own model nuclei")) {
            nuclei_channel = 1;
            cyto_channel = -1;
        } else if (model.equals("own model bact_omni")) {
            nuclei_channel = -1;
            cyto_channel = 1;
        } else if ((model.equals("own model cyto")) || (model.equals("own model cyto2")) || (model.equals("own model cyto2_omni"))) {
            cyto_channel = 1;
            nuclei_channel = 2;
        }
    }


    @Override
    public void run() {
        // Prepare cellPose settings
        CellposeTaskSettings settings = new CellposeTaskSettings();
        // this is necessary
        settings.setFromPrefs();
        // and a cellpose task
        DefaultCellposeTask cellposeTask = new DefaultCellposeTask();

        Calibration cal = imp.getCalibration();

        // We'll ave the current time-point of the imp in a temp folder
        String tempDir = IJ.getDirectory("Temp");
        // create tempdir
        File cellposeTempDir = new File(tempDir, "cellposeTemp");
        cellposeTempDir.mkdir();

        // Add it to the settings
        settings.setDatasetDir(cellposeTempDir.toString());

        if (model.equals("nuclei")) {
            settings.setChannel1(nuclei_channel);
            //settings.setChannel2(-1) ;
        } else if ((model.equals("bact_omni"))) {
            settings.setChannel1(cyto_channel);
        } else if ((model.equals("cyto")) || (model.equals("cyto2")) || (model.equals("cyto2_omni"))) {
            System.out.println("cyto_channel:" + cyto_channel + ":nuclei_channel:" + nuclei_channel);
            settings.setChannel1(cyto_channel);
            settings.setChannel2(nuclei_channel);
        } else if ((model.equals("own model nuclei"))) {
            model = model_path.toString();
            settings.setChannel1(nuclei_channel);
        } else if ((model.equals("own model bact_omni"))) {
            model = model_path.toString();
            settings.setChannel1(cyto_channel);
        } else if ((model.equals("own model cyto")) || (model.equals("own model cyto2")) || (model.equals("own model cyto2_omni"))) {
            model = model_path.toString();
            settings.setChannel1(cyto_channel);
            settings.setChannel2(nuclei_channel);
        }

        settings.setModel(model);

        settings.setDiameter(diameter);
        settings.setCellProbTh(cellproba_threshold);
        settings.setFlowTh(flow_threshold);
        settings.setAnisotropy(anisotropy);
        settings.setStitchThreshold(stitch_threshold);
        settings.setOmni(omni);
        settings.setCluster(cluster);
        settings.setAdditionalFlags(additionnal_flags);

        if (dimensionMode.equals("3D")) {
            if (imp.getNSlices() > 1) settings.setDo3D();
            else System.out.println("NOTE : Can't use 3D mode, on 2D image");
        }

        // settings are done , so we can now process the imp with cellpose
        cellposeTask.setSettings(settings);
        try {
            // can't process time-lapse directly so, we'll save one time-point after another
            int impFrames = imp.getNFrames();

            // we'll use list to store paths of saved input, output masks and outlines
            List<File> t_imp_paths = new ArrayList<>();
            List<File> cellpose_masks_paths = new ArrayList<>();
            List<File> cellpose_outlines_paths = new ArrayList<>();

            for (int t_idx = 1; t_idx <= impFrames; t_idx++) {
                // duplicate all channels and all z-slices for a defined time-point
                ImagePlus t_imp = new Duplicator().run(imp, 1, imp.getNChannels(), 1, imp.getNSlices(), t_idx, t_idx);
                // and save the current t_imp into the cellposeTempDir
                File t_imp_path = new File(cellposeTempDir, imp.getShortTitle() + "-t" + t_idx + ".tif");
                FileSaver fs = new FileSaver(t_imp);
                fs.saveAsTiff(t_imp_path.toString());
                if (verbose) System.out.println(t_imp_path.toString());
                // add to list of paths to delete at the end of operations
                t_imp_paths.add(t_imp_path);

                // prepare path of the cellpose mask output
                File cellpose_imp_path = new File(cellposeTempDir, imp.getShortTitle() + "-t" + t_idx + "_cp_masks" + ".tif");
                cellpose_masks_paths.add(cellpose_imp_path);
                // cellpose also creates a txt file (probably to be used with a script to import ROI in imagej), we'll delete it too
                // (to generate ROIs from the label image we can use https://github.com/BIOP/ijp-larome)
                File cellpose_outlines_path = new File(cellposeTempDir, imp.getShortTitle() + "-t" + t_idx + "_cp_outlines" + ".txt");
                cellpose_outlines_paths.add(cellpose_outlines_path);
            }

            // RUN CELLPOSE !
            cellposeTask.run();

            // Open all the cellpose_mask and store each imp within an ArrayList
            ArrayList<ImagePlus> imps = new ArrayList<>(impFrames);
            for (int t_idx = 1; t_idx <= impFrames; t_idx++) {
                ImagePlus cellpose_t_imp = IJ.openImage(cellpose_masks_paths.get(t_idx - 1).toString());
                imps.add(cellpose_t_imp.duplicate());
            }
            // Convert the ArrayList to an imp
            // https://stackoverflow.com/questions/9572795/convert-list-to-array-in-java
            ImagePlus[] impsArray = imps.toArray(new ImagePlus[0]);
            cellpose_imp = Concatenator.run(impsArray);
            cellpose_imp.setCalibration(cal);
            cellpose_imp.setTitle(imp.getShortTitle() + "-cellpose");

            // Delete the created files and folder
            for (int t_idx = 1; t_idx <= impFrames; t_idx++) {
                t_imp_paths.get(t_idx - 1).delete();
                cellpose_masks_paths.get(t_idx - 1).delete();
                cellpose_outlines_paths.get(t_idx - 1).delete();
            }
            cellposeTempDir.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void main(final String... args) {

        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        // will run on the current image
        //ij.command().run(CellposePrefsSet.class, true);
        ij.command().run(Cellpose_SegmentImgPlusOwnModelAdvanced.class, true);

    }
}