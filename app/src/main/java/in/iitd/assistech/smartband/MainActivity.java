package in.iitd.assistech.smartband;

import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;
import java.util.Arrays;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

public class MainActivity extends AppCompatActivity {

    static double[][] inputWeights;
    static double[][] layerWeights;
    static double[][] inputBias;
    static double[][] outputBias;

    double[][] inputFeat;

    double hornProb;
    double cryProb;
    double ambientProb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try{
            AssetManager am = getAssets();
            InputStream is = am.open("NN_Weights.xls");
            final Workbook wb = Workbook.getWorkbook(is);

            Sheet inputWeightSheet = wb.getSheet("InputWeight");
            int inputWeightSheetRows = inputWeightSheet.getRows();
            System.out.println(inputWeightSheetRows);
            int inputWeightSheetColumns = inputWeightSheet.getColumns();
            inputWeights = new double[inputWeightSheetRows][inputWeightSheetColumns];

            for (int i = 0; i<inputWeightSheetRows; i++){
                for (int j=0; j<inputWeightSheetColumns; j++){
                    Cell z = inputWeightSheet.getCell(j, i);
                    inputWeights[i][j] = Double.parseDouble(z.getContents());
                }
            }

            Sheet layerWeightSheet = wb.getSheet("LayerWeight");
            int layerWeightSheetRows = layerWeightSheet.getRows();
            int layerWeightSheetColumns = layerWeightSheet.getColumns();
            layerWeights = new double[layerWeightSheetRows][layerWeightSheetColumns];

            for (int i = 0; i<layerWeightSheetRows; i++){
                for (int j=0; j<layerWeightSheetColumns; j++){
                    Cell z = layerWeightSheet.getCell(j, i);
                    layerWeights[i][j] = Double.parseDouble(z.getContents());
                }
            }

            Sheet inputBiasSheet = wb.getSheet("InputBias");
            int inputBiasSheetRows = inputBiasSheet.getRows();
            int inputBiasSheetColumns = inputBiasSheet.getColumns();
            inputBias = new double[inputBiasSheetRows][inputBiasSheetColumns];

            for (int i=0; i<inputBiasSheetRows; i++){
                for (int j=0; j<inputBiasSheetColumns; j++){
                    Cell z = inputBiasSheet.getCell(j, i);
                    inputBias[i][j] = Double.parseDouble(z.getContents());
                }
            }

            Sheet outputBiasSheet = wb.getSheet("OutputBias");
            int outputBiasSheetRows = outputBiasSheet.getRows();
            int outputBiasSheetColumns = outputBiasSheet.getColumns();
            outputBias = new double[outputBiasSheetRows][outputBiasSheetColumns];

            for (int i=0; i<outputBiasSheetRows; i++){
                for (int j=0; j<outputBiasSheetColumns; j++){
                    Cell z = outputBiasSheet.getCell(j, i);
                    outputBias[i][j] = Double.parseDouble(z.getContents());
                }
            }

            Sheet inputFeatSheet = wb.getSheet("InputFeat");
            int inputFeatSheetRows = inputFeatSheet.getRows();
            int inputFeatSheetColumns = inputFeatSheet.getColumns();
            inputFeat = new double[inputFeatSheetRows][inputFeatSheetColumns];

            for (int i=0; i<inputFeatSheetRows; i++){
                for (int j=0; j<inputFeatSheetColumns; j++){
                    Cell z = inputFeatSheet.getCell(j, i);
                    inputFeat[i][j] = Double.parseDouble(z.getContents());
                }
            }

            System.out.println("arr: " + Arrays.toString(inputBias));
            System.out.println("inputWeightSize: "+ Integer.toString(inputBias.length));
        } catch (Exception e){
        }
    }

    public void loadWeight(View v){
        //displayWeight(inputFeat);
    }

    public void calcOutput(View v){
        TextView hornValue = (TextView)findViewById(R.id.hornValue);
        TextView cryValue = (TextView)findViewById(R.id.cryValue);
        TextView ambientValue = (TextView)findViewById(R.id.ambientValue);

        double[] hiddenNodes = new double[inputWeights.length];
        for (int i=0; i<inputWeights.length; i++){
            double sum = 0;
            for (int j=0; j<inputWeights[0].length; j++){
                sum += inputWeights[i][j]*inputFeat[j][0];
            }
            hiddenNodes[i] = tansig(sum + inputBias[i][0]);
        }

        double[] outputNodes = new double[layerWeights.length];
        for (int i=0; i<layerWeights.length; i++){
            double sum = 0;
            for (int j=0; j<layerWeights[0].length; j++){
                sum += layerWeights[i][j]*hiddenNodes[j];
            }
            outputNodes[i] = sum + outputBias[i][0];
        }

        displayWeight(outputNodes);

        double sum = 0.0;
        for (int i=0; i<outputNodes.length; i++){
            sum += Math.exp(Math.abs(outputNodes[i]));
        }

        for (int i=0; i<outputNodes.length; i++){
            outputNodes[i] = softmax(outputNodes[i], sum);
        }

        hornProb = outputNodes[0];
        cryProb = outputNodes[2];
        ambientProb = outputNodes[1];

        hornValue.setText(Double.toString(hornProb));
        cryValue.setText(Double.toString(cryProb));
        ambientValue.setText(Double.toString(ambientProb));
    }

    public void displayWeight(double[] value){
        TextView inputWeight = (TextView)findViewById(R.id.inputWeight);
        //inputWeight.setText(Double.toString(value[0][0]));
        for(int i=0; i<value.length; i++){
            //for(int j=0; j<value.length; j++){
                inputWeight.append(Double.toString(value[i]));
                inputWeight.append("\n");
            //}
        }
    }

    private double tansig(double x) {
        return ((2.0 / (1 + Math.exp(-2*x))) - 1.0);
    }

    private double softmax(double outputNode, double sum) {
        double temp = Math.exp(Math.abs(outputNode));
        System.out.println("temp " + Double.toString(outputNode)+ ","+ Double.toString(temp));
        System.out.println("SUM " + Double.toString(sum));
        return (temp/sum);
    }
}