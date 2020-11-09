package app;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.deident.DeIdentifier;
import org.dcm4che3.io.DicomEncodingOptions;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.tool.common.CLIUtils;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class Deidentify {

    private final DeIdentifier deidentifier;

    public Deidentify(DeIdentifier.Option... options) {
        deidentifier = new DeIdentifier(options);
    }

    public static void main(String[] args) {
        try {
            DeIdentifier.Option[] options = new DeIdentifier.Option[]{
                    DeIdentifier.Option.RetainUIDsOption,
//                    DeIdentifier.Option.RetainInstitutionIdentityOption,
//                    DeIdentifier.Option.RetainDeviceIdentityOption,
//                    DeIdentifier.Option.RetainLongitudinalTemporalInformationFullDatesOption,
//                    DeIdentifier.Option.BasicApplicationConfidentialityProfile
            };

            Map<String, String> dummyValues = new HashMap<>();
            dummyValues.put("PatientName", "ANONYMIZED");

            String srcPath = "./data/in";
            String destPath = "./data/out";

            Deidentify main = new Deidentify(options);
            main.setDummyValues(dummyValues);

            main.mtranscode(new File(srcPath), new File(destPath));
        } catch (Exception e) {
            System.err.println("deidentify: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private void setDummyValues(Map<String, String> dummyValues) {
        for (Map.Entry<String, String> dummyValue : dummyValues.entrySet()) {
            int tag = CLIUtils.toTag(dummyValue.getKey());
            VR vr = ElementDictionary.getStandardElementDictionary().vrOf(tag);
            deidentifier.setDummyValue(tag, vr, dummyValue.getValue());
        }
    }

    private void mtranscode(File src, File dest) {
        if (src.isDirectory()) {
            dest.mkdir();
            for (File file : src.listFiles())
                mtranscode(file, new File(dest, file.getName()));
            return;
        }
        if (dest.isDirectory())
            dest = new File(dest, src.getName());
        try {
            transcode(src, dest);
            System.out.println(
                    MessageFormat.format("{0} -> {1}",
                            src, dest));
        } catch (Exception e) {
            System.out.println(
                    MessageFormat.format("Failed to de-identify {0}: {1}",
                            src, e.getMessage()));
            e.printStackTrace(System.out);
        }
    }

    public void transcode(File src, File dest) throws IOException {
        Attributes fmi;
        Attributes dataset;
        try (DicomInputStream dis = new DicomInputStream(src)) {
            dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.URI);
            fmi = dis.readFileMetaInformation();
            dataset = dis.readDataset(-1, -1);
        }
        deidentifier.deidentify(dataset);
        if (fmi != null)
            fmi = dataset.createFileMetaInformation(fmi.getString(Tag.TransferSyntaxUID));
        try (DicomOutputStream dos = new DicomOutputStream(dest)) {
            dos.setEncodingOptions(DicomEncodingOptions.DEFAULT);
            dos.writeDataset(fmi, dataset);
        }
    }
}
