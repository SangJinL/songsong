package com.example.songsong;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private int wrongQuestionsCount = 100;

    private TextView hintTextView;
    private ImageView imageView;
    private TextView songTitleTextView;
    private TextView artistTextView;
    private TextView timerTextView;
    private TextView remainingQuestionsTextView;
    private EditText songEditText;
    private EditText artistEditText;
    private TextView correctCountTextView;
    private TextView wrongCountTextView;
    private Button checkButton;
    private LoadImageTask loadImageTask;
    private List<Song> songList;
    private List<Song> remainingQuestions;
    private boolean hint1Shown = false;
    private boolean hint2Shown = false;
    private Song currentSong;
    private int answeredQuestions;
    private int totalQuestions;
    private int wrongQuestions = 0;
    private CountDownTimer timer;
    private MediaPlayer mediaPlayer;
    private int correctQuestions;
    private boolean isCorrect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        correctCountTextView = findViewById(R.id.correctCountTextView);
        wrongCountTextView = findViewById(R.id.wrongCountTextView);
        imageView = findViewById(R.id.imageView);
        songTitleTextView = findViewById(R.id.songTitleTextView);
        artistTextView = findViewById(R.id.artistTextView);
        timerTextView = findViewById(R.id.timerTextView);
        remainingQuestionsTextView = findViewById(R.id.remainingQuestionsTextView);
        songEditText = findViewById(R.id.songEditText);
        artistEditText = findViewById(R.id.artistEditText);
        checkButton = findViewById(R.id.checkButton);

        songList = loadSongListFromCSV();
        if (songList == null) {
            Toast.makeText(this, "Failed to load song list", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        remainingQuestions = new ArrayList<>(songList);
        answeredQuestions = 0;
        totalQuestions = songList.size();
        wrongQuestions = 0;

        mediaPlayer = new MediaPlayer();

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer();
            }
        });

        hintTextView = findViewById(R.id.hintTextView);

        startGame();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        releaseMediaPlayer();
        cancelLoadImageTask();
        finish();
    }

    private void cancelLoadImageTask() {
        if (loadImageTask != null) {
            loadImageTask.cancel(true);
            loadImageTask = null;
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        releaseMediaPlayer();
        cancelLoadImageTask();
        finish();
    }

    private void startGame() {
        hint1Shown = false;
        hint2Shown = false;

        if (remainingQuestions.isEmpty()) {
            finishGame();
            return;
        }

        currentSong = getRandomSong();
        if (currentSong != null) {
            songTitleTextView.setText("");
            artistTextView.setText("");

            int remainingQuestionCount = remainingQuestions.size();
            remainingQuestionsTextView.setText("남은 문제: " + remainingQuestionCount + "/" + totalQuestions);

            correctCountTextView.setText("맞힌 문제: " + correctQuestions);
            wrongCountTextView.setText("틀린 문제: " + wrongQuestions);

            if (timer != null) {
                timer.cancel();
            }
            timer = new CountDownTimer(90000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timerTextView.setText(String.valueOf(millisUntilFinished / 1000));

                    if (!hint1Shown && millisUntilFinished <= 60000) {
                        showHint(currentSong.getHint1());
                        hint1Shown = true;
                    } else if (!hint2Shown && millisUntilFinished <= 30000) {
                        showHint(currentSong.getHint2());
                        hint2Shown = true;
                    }
                    if (millisUntilFinished <= 15000) {
                        showImage();
                    } else {
                        hideImage();
                    }
                }

                @Override
                public void onFinish() {
                    timerTextView.setText("시간초과!");
                    Toast.makeText(GameActivity.this, "오답입니다!", Toast.LENGTH_SHORT).show();
                    answeredQuestions++;
                    wrongQuestions++; // 틀린 문제 수 증가
                    showResult();
                    remainingQuestions.remove(currentSong); // 남은 문제 수 감소
                    nextQuestion();
                }
            }.start();

            try {
                String songFileName = currentSong.getFilename();
                AssetFileDescriptor afd = getResources().openRawResourceFd(getResources().getIdentifier(songFileName, "raw", getPackageName()));

                mediaPlayer.reset();
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });
                mediaPlayer.prepareAsync();

                String imageLink = currentSong.getImageLink();
                if (!imageLink.isEmpty()) {
                    // 이미지를 가져오는 AsyncTask를 실행
                    new LoadImageTask(imageView).execute(imageLink);
                } else {
                    hideImage();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            clearHints();
        }
    }

    private void showImage() {
        imageView.setVisibility(View.VISIBLE);
    }

    private void hideImage() {
        imageView.setVisibility(View.GONE);
    }

    private void clearHints() {
        hintTextView.setText("");
    }

    private void nextQuestion() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();

        remainingQuestions.remove(currentSong); // 현재 문제를 제거합니다.

        // 텍스트 필드를 비웁니다.
        songEditText.setText("");
        artistEditText.setText("");
        startGame();
    }

    private void checkAnswer() {
        if (currentSong == null) {
            Toast.makeText(this, "게임이 아직 시작되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userSong = songEditText.getText().toString().trim();
        String userArtist = artistEditText.getText().toString().trim();

        if (userSong.equalsIgnoreCase(currentSong.getSong())
                && userArtist.equalsIgnoreCase(currentSong.getSinger())) {
            Toast.makeText(this, "정답입니다!", Toast.LENGTH_SHORT).show();
            correctQuestions++; // 맞힌 문제 개수 증가
            remainingQuestions.remove(currentSong); // 정답을 맞혔으므로 현재 문제를 남은 문제에서 제외합니다.
            if (timer != null) {
                timer.cancel();
            }
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            nextQuestion(); // 정답을 맞혔으므로 다음 문제로 넘어갑니다.
        } else {
            if (timerTextView.getText().equals("시간초과!")) {
                Toast.makeText(this, "시간이 초과되어 다음 문제로 넘어갑니다.", Toast.LENGTH_SHORT).show();
                remainingQuestions.remove(currentSong); // 시간이 초과되었으므로 현재 문제를 남은 문제에서 제외합니다.
                wrongQuestions++; // 틀린 문제 개수 증가
                if (timer != null) {
                    timer.cancel();
                }
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                nextQuestion(); // 시간이 초과되었으므로 다음 문제로 넘어갑니다.
                remainingQuestions.remove(currentSong); // 현재 문제를 제거합니다.
            } else {
                Toast.makeText(this, "오답입니다!", Toast.LENGTH_SHORT).show();
                // 오답을 입력했으므로 입력 필드를 초기화하고 같은 문제를 계속 유지합니다.
                songEditText.setText("");
                artistEditText.setText("");
            }
        }
    }
    private void showResult() {
        int correctAnswers = answeredQuestions;
        int wrongAnswers = wrongQuestions;

        correctCountTextView.setText("맞힌 문제: " + correctAnswers);
        wrongCountTextView.setText("틀린 문제: " + wrongAnswers);
    }

    private void showHint(String hint) {
        hintTextView.setText("힌트: " + hint);
        hintTextView.setVisibility(View.VISIBLE);
    }

    private void finishGame() {
        releaseMediaPlayer();
        cancelLoadImageTask();

        // 결과 액티비티로 이동
        Intent intent = new Intent(GameActivity.this, ResultActivity.class);
        intent.putExtra("correctCount", correctQuestions);
        intent.putExtra("wrongCount", wrongQuestions);
        startActivity(intent);
        finish();
    }


    private List<Song> loadSongListFromCSV() {
        List<Song> songList = new ArrayList<>();

        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = getResources().openRawResource(R.raw.song);
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // Skip the first line (header row)
            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 7) {
                    int number = Integer.parseInt(data[0]);
                    String song = data[1];
                    String singer = data[2];
                    String fileName = data[3].replace(".mp3", "");  // Remove the file extension
                    String hint1 = data[4];
                    String hint2 = data[5];
                    String imageLink = data[6];  // 이미지 링크 추가

                    Song newSong = new Song(number, song, singer, fileName, hint1, hint2, imageLink);
                    songList.add(newSong);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return songList.size() > 0 ? songList : null;
    }

    private Song getRandomSong() {
        if (remainingQuestions.isEmpty()) {
            return null;
        }

        Random random = new Random();
        int randomIndex = random.nextInt(remainingQuestions.size());
        return remainingQuestions.get(randomIndex);
    }

    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;

        LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String imageUrl = urls[0];
            Bitmap bitmap = null;
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }
    }
}
