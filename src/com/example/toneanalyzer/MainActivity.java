package com.example.toneanalyzer;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.widget.*;
import android.os.Environment;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;



public class MainActivity extends ActionBarActivity {

	private MediaRecorder mRecorder;
	private String tempFileName = null;
	private String outFileName = null;
	
	private static final int RECORDER_BPP = 16;
    //private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    //private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    //private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	
    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    ArrayList<Double> frequencyValues= new ArrayList<Double>();
    private double frequencyValue=0.0;
    //private int freqSamples=0;
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	
	/** Called when the user clicks the button Start_Recording */
	public void startRecording(View view) {
		
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		tempFileName = Environment.getExternalStorageDirectory() + "/Tones/audio_"+timeStamp + ".raw";
		outFileName = Environment.getExternalStorageDirectory() + "/Tones/audio_"+timeStamp + ".wav";
		/*
		mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Log.i("mediaPrepare", "prepare() failed");
        }
		*/
		
		frequencyValue=0;
		frequencyValues.clear();
		//freqSamples=0;
		
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

		int i = recorder.getState();
		if(i==1)
		recorder.startRecording();
		
		isRecording = true;
		
		recordingThread = new Thread(new Runnable() {
		
		@Override
		public void run() {
		writeAudioDataToFile();
		}
		},"AudioRecorder Thread");
		
		recordingThread.start();

	}
	
	 private void writeAudioDataToFile(){
         byte data[] = new byte[bufferSize];
         String filename = tempFileName;
         FileOutputStream os = null;
         
         try {
                 os = new FileOutputStream(filename);
         } catch (FileNotFoundException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
         }
         
         int read = 0;
         
         if(null != os){
                 while(isRecording){
                         read = recorder.read(data, 0, bufferSize);
                         
                         if(read > 0){
                        	 frequencyValue=calculateFFT(data);
                        	 if (frequencyValue>199.99){
                        		 frequencyValues.add(frequencyValue);
                                 //freqSamples++;
                        	 }
                             
                         }
                         
                         if(AudioRecord.ERROR_INVALID_OPERATION != read){
                                 try {
                                         os.write(data);
                                 } catch (IOException e) {
                                         e.printStackTrace();
                                 }
                         }
                 }
                 
                 try {
                         os.close();
                 } catch (IOException e) {
                         e.printStackTrace();
                 }
         }
 }
	 
	  private void deleteTempFile() {
          File file = new File(tempFileName);
          
          file.delete();
  }
	 
	 private void copyWaveFile(String inFilename,String outFilename){
         FileInputStream in = null;
         FileOutputStream out = null;
         long totalAudioLen = 0;
         long totalDataLen = totalAudioLen + 36;
         long longSampleRate = RECORDER_SAMPLERATE;
         int channels = 2;
         long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
         
         byte[] data = new byte[bufferSize];
         
         try {
                 in = new FileInputStream(inFilename);
                 out = new FileOutputStream(outFilename);
                 totalAudioLen = in.getChannel().size();
                 totalDataLen = totalAudioLen + 36;
                 
                 //AppLog.logString("File size: " + totalDataLen);
                 
                 WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                                 longSampleRate, channels, byteRate);
                 
                 while(in.read(data) != -1){
                         out.write(data);
                 }
                 
                 in.close();
                 out.close();
         } catch (FileNotFoundException e) {
                 e.printStackTrace();
         } catch (IOException e) {
                 e.printStackTrace();
         }
 }

 private void WriteWaveFileHeader(
                 FileOutputStream out, long totalAudioLen,
                 long totalDataLen, long longSampleRate, int channels,
                 long byteRate) throws IOException {
         
         byte[] header = new byte[44];
         
         header[0] = 'R';  // RIFF/WAVE header
         header[1] = 'I';
         header[2] = 'F';
         header[3] = 'F';
         header[4] = (byte) (totalDataLen & 0xff);
         header[5] = (byte) ((totalDataLen >> 8) & 0xff);
         header[6] = (byte) ((totalDataLen >> 16) & 0xff);
         header[7] = (byte) ((totalDataLen >> 24) & 0xff);
         header[8] = 'W';
         header[9] = 'A';
         header[10] = 'V';
         header[11] = 'E';
         header[12] = 'f';  // 'fmt ' chunk
         header[13] = 'm';
         header[14] = 't';
         header[15] = ' ';
         header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
         header[17] = 0;
         header[18] = 0;
         header[19] = 0;
         header[20] = 1;  // format = 1
         header[21] = 0;
         header[22] = (byte) channels;
         header[23] = 0;
         header[24] = (byte) (longSampleRate & 0xff);
         header[25] = (byte) ((longSampleRate >> 8) & 0xff);
         header[26] = (byte) ((longSampleRate >> 16) & 0xff);
         header[27] = (byte) ((longSampleRate >> 24) & 0xff);
         header[28] = (byte) (byteRate & 0xff);
         header[29] = (byte) ((byteRate >> 8) & 0xff);
         header[30] = (byte) ((byteRate >> 16) & 0xff);
         header[31] = (byte) ((byteRate >> 24) & 0xff);
         header[32] = (byte) (2 * 16 / 8);  // block align
         header[33] = 0;
         header[34] = RECORDER_BPP;  // bits per sample
         header[35] = 0;
         header[36] = 'd';
         header[37] = 'a';
         header[38] = 't';
         header[39] = 'a';
         header[40] = (byte) (totalAudioLen & 0xff);
         header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
         header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
         header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

         out.write(header, 0, 44);
 }
	
	/** Called when the user clicks the button Start_Recording */
	public void startFFT(View view) {
		/*
		mRecorder.stop();
        mRecorder.release();
        Recorder = null;
        */
        
        if(null != recorder){
            isRecording = false;
            
            int i = recorder.getState();
            if(i==1)
            	recorder.stop();
            recorder.release();
            
            recorder = null;
            recordingThread = null;
        }
        
        // Write the result
        double freqOutput = calcMedian();
        String result_String = freqOutput + "Hz";
		((TextView)findViewById (R.id.editText1)).setText (result_String);
        //copyWaveFile(tempFileName, outFileName);
        deleteTempFile();
		
	}
	
	// Calculate the median of frequency values
	double calcMedian()
	{
		double median;
		Collections.sort(frequencyValues);
		int length = frequencyValues.size();
		if (length%2 == 0){
			median = (frequencyValues.get((length-1)/2)+frequencyValues.get((length)/2))/2.0;
		}
		else{
			median = frequencyValues.get((length-1)/2);
		}
		
		return median;
	}
	
	// Calculate FFT for the studio signal
	public double calculateFFT(byte[] signal)
    {           
        final int mNumberOfFFTPoints =1024;
        double mMaxFFTSample;

        double temp;
        double frequency;
        int mPeakPos;
        Complex[] y;
        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
        double[] absSignal = new double[mNumberOfFFTPoints/2];

        for(int i = 0; i < mNumberOfFFTPoints; i++){
            temp = (double)((signal[2*i] & 0xFF) | (signal[2*i+1] << 8)) / 32768.0F;
            complexSignal[i] = new Complex(temp,0.0);
        }

        y = FFT.fft(complexSignal); // --> Here I use FFT class

        mMaxFFTSample = 0.0;
        mPeakPos = 0;
        for(int i = 0; i < (mNumberOfFFTPoints/2); i++)
        {
             absSignal[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
             if(absSignal[i] > mMaxFFTSample)
             {
                 mMaxFFTSample = absSignal[i];
                 mPeakPos = i;
             } 
        }
        
        frequency = mPeakPos * RECORDER_SAMPLERATE/mNumberOfFFTPoints;
        
        //System.out.println(frequency);

        return frequency;

    }
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
