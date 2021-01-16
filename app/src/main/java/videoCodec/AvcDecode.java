package videoCodec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * h264解码器，相对编码器要简单
 **/
public class AvcDecode {
    /** 这里是建立的解码器 */
    MediaCodec mediaCodec = null;
    ByteBuffer[] inputBuffers = null;
    /** 帧率 */
    int m_framerate = 10;
    /** pts时间基数 */
    long presentationTimeUs = 0;

    public AvcDecode(int mWidth, int mHeigh, Surface surface) {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeigh);
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mediaCodec.configure(mediaFormat, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        inputBuffers = mediaCodec.getInputBuffers();
    }

//    public byte[] decodeH264(byte[] h264,int length) {
//        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
//        int inputBufferIndex = mediaCodec.dequeueInputBuffer(100);//-1表示等待
//        if (inputBufferIndex >= 0) {
//            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
//            inputBuffer.clear();
////            inputBuffer.p
//            inputBuffer.put(h264,0,length);
//            //计算pts
//            long pts = computePresentationTime(presentationTimeUs);
//            mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, pts, 0);
//            presentationTimeUs += 1;
//        } else {
//            return null;
//        }
//        // Get output buffer index
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
//        while (outputBufferIndex >= 0) {
//            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);//到这里为止应该有图像显示了
//            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100);
//        }
//        Log.e("Media", "onFrame end :"+outputBufferIndex+" inputBufferIndex:"+inputBufferIndex);
//        return h264;
//
//    }

    public byte[] decodeH264(byte[] h264,int length) {
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(100);//-1表示等待
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
//            inputBuffer.p
            inputBuffer.put(h264,0,length);
            //计算pts
            long pts = computePresentationTime(presentationTimeUs);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, pts, 0);
            presentationTimeUs += 1;
        } else {
            return null;
        }
        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100);
        do
            {
//            switch (outputBufferIndex) {
//                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
//                    Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
////                    outputBuffers = decoder.getOutputBuffers();
//                    break;
//                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
//                    Log.d("DecodeActivity", "New format dequeueInputBuffer " + mediaCodec.getOutputFormat());
////					        mStreamSrcWidth =  decoder.getOutputFormat().get
//                    MediaFormat  myStreamFormat = mediaCodec.getOutputFormat();
//                    if(myStreamFormat!=null){
//                        int mStreamSrcWidth = myStreamFormat.getInteger(MediaFormat.KEY_WIDTH);
//                        int mStreamSrcHeigth = myStreamFormat.getInteger(MediaFormat.KEY_HEIGHT);
//                        System.out.println( "2.New format iHeight:" +mStreamSrcHeigth + " height:" +mStreamSrcWidth);
//
//                    }
//                    break;
//                case MediaCodec.INFO_TRY_AGAIN_LATER:
//                    break;
//                default:
//                    break;
////					    	System.out.println("mHandler.sendMessage");
//
//            }
            if(outputBufferIndex>=0)
                mediaCodec.releaseOutputBuffer(outputBufferIndex, true);//到这里为止应该有图像显示了
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        while (outputBufferIndex >= 0);
        Log.e("Media", "onFrame end :"+outputBufferIndex+" inputBufferIndex:"+inputBufferIndex);
        return h264;

    }


    public ByteBuffer[] decodeH264ToData(byte[] h264) {
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        /** -1表示等待 */
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(h264);
            //计算pts
            long pts = computePresentationTime(presentationTimeUs);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, h264.length,0,0);
            presentationTimeUs += 1;

        } else {
            return null;
        }
        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100);
        do
        {
//            switch (outputBufferIndex) {
//                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
//                    Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
////                    outputBuffers = decoder.getOutputBuffers();
//                    break;
//                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
//                    Log.d("DecodeActivity", "New format dequeueInputBuffer " + mediaCodec.getOutputFormat());
////					        mStreamSrcWidth =  decoder.getOutputFormat().get
//                    MediaFormat  myStreamFormat = mediaCodec.getOutputFormat();
//                    if(myStreamFormat!=null){
//                        int mStreamSrcWidth = myStreamFormat.getInteger(MediaFormat.KEY_WIDTH);
//                        int mStreamSrcHeigth = myStreamFormat.getInteger(MediaFormat.KEY_HEIGHT);
//                        System.out.println( "2.New format iHeight:" +mStreamSrcHeigth + " height:" +mStreamSrcWidth);
//
//                    }
//                    break;
//                case MediaCodec.INFO_TRY_AGAIN_LATER:
//                    break;
//                default:
//                    break;
////					    	System.out.println("mHandler.sendMessage");
//
//            }
            if(outputBufferIndex>=0)
                mediaCodec.releaseOutputBuffer(outputBufferIndex, true);//到这里为止应该有图像显示了
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        while (outputBufferIndex >= 0);
        Log.e("Media", "decodeH264ToData onFrame end" + outputBufferIndex);
        return mediaCodec.getOutputBuffers();
    }
    /**
     * 计算pts
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / m_framerate;
    }
}
