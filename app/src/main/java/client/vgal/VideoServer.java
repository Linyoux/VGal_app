package client.vgal;

import android.os.Environment;
import fi.iki.elonen.NanoHTTPD;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class VideoServer extends NanoHTTPD {

    public VideoServer(int port) throws IOException {
        super(port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (uri.equals("/video.mp4")) {
            String videoFilePath = new File(Environment.getExternalStorageDirectory(),"vgal/game.mp4").getAbsolutePath();
            try {
                FileInputStream fis = new FileInputStream(videoFilePath);
                return newChunkedResponse(Response.Status.OK, "video/mp4", fis);
            } catch (IOException ioe) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + ioe.getMessage());
            }
        } else {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        }

    }
}
