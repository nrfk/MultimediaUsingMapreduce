package fr.telecomParistech.dash.util;


import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;


public class MPDConcatenator {
//	public void concat(String initSegmentUrl, String mediaSegmentUrl,
//			String outputName) {
//		try {
//			File file = new File(initSegmentUrl);
//			byte[] initSegment = new byte[(int) file.length()];
//			DataInputStream dis = new DataInputStream(new FileInputStream(file));
//			dis.readFully(initSegment);
//			dis.close();
//
//			file = new File(mediaSegmentUrl);
//			byte[] mediaSegment = new byte[(int) file.length()];
//			dis = new DataInputStream(new FileInputStream(file));
//			dis.readFully(mediaSegment);
//			dis.close();
//
//			byte[] output = new byte[initSegment.length + mediaSegment.length];
//			System.arraycopy(initSegment, 0, output, 0, initSegment.length);
//			System.arraycopy(mediaSegment, 0, output, initSegment.length,
//					mediaSegment.length);
//
//			File outputFile = new File(outputName);
//			OutputStream fos = new FileOutputStream(outputFile);
//			try {
//				fos.write(output);
//			} finally {
//				fos.close();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		MPDConcatenator mpdConcatenator = new MPDConcatenator();
//		// MPDReader mpdReader = new MPDReader();
//		// mpdReader.readMPD("resource/count-video_dash.mpd");
//
//		mpdConcatenator.concat("resource/count-video_dash.mp4",
//				"resource/seg2.m4s", "resource/out.mp4");
//
//		System.out.println("Done");
//	}
}
