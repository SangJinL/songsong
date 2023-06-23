package com.example.songsong;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private TextView hintTextView;
    private TextView songTitleTextView;
    private TextView artistTextView;
    private TextView timerTextView;
    private TextView remainingQuestionsTextView;
    private EditText songEditText;
    private EditText artistEditText;
    private Button checkButton;

    private List<Song> songList;
    private List<Song> remainingQuestions;
    private boolean hint1Shown = false;
    private boolean hint2Shown = false;
    private Song currentSong;
    private int answeredQuestions;
    private int totalQuestions;
    private Button volumeUpButton;
    private Button volumeDownButton;
    private AudioManager audioManager;

    private CountDownTimer timer;
    private MediaPlayer mediaPlayer;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        songTitleTextView = findViewById(R.id.songTitleTextView);
        artistTextView = findViewById(R.id.artistTextView);
        timerTextView = findViewById(R.id.timerTextView);
        remainingQuestionsTextView = findViewById(R.id.remainingQuestionsTextView);
        songEditText = findViewById(R.id.songEditText);
        artistEditText = findViewById(R.id.artistEditText);
        checkButton = findViewById(R.id.checkButton);

        songList = loadSongListFromCSV();
        remainingQuestions = new ArrayList<>(songList);
        answeredQuestions = 0;
        totalQuestions = songList.size();

        /*volumeUpButton = findViewById(R.id.volumeUpButton);
        volumeDownButton = findViewById(R.id.volumeDownButton);*/
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaPlayer = new MediaPlayer();

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer();
            }
        });

        hintTextView = findViewById(R.id.hintTextView);

        startGame();

       /* volumeUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseVolume();
            }
        });

        volumeDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseVolume();
            }
        });*/
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
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
            songTitleTextView.setText("");  // 가사 제목 숨기기
            artistTextView.setText("");  // 가수 이름 숨기기

            int remainingQuestionCount = remainingQuestions.size();
            remainingQuestionsTextView.setText("남은 문제: " + remainingQuestionCount + "/" + totalQuestions);

            if (timer != null) {
                timer.cancel();
            }
            timer = new CountDownTimer(60000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timerTextView.setText(String.valueOf(millisUntilFinished / 1000));

                    if (!hint1Shown && millisUntilFinished <= 40000) { // 40초 남았을 때 힌트 출력
                        showHint(currentSong.getHint1());
                        hint1Shown = true;
                    }
                    else if (!hint2Shown && millisUntilFinished <= 20000) { // 20초 남았을 때 힌트 출력
                        showHint(currentSong.getHint2());
                        hint2Shown = true;
                    }
                }

                @Override
                public void onFinish() {
                    timerTextView.setText("시간초과!");  // 시간 초과 메시지 표시
                    nextQuestion();  // 다음 문제로 넘어가는 함수를 호출
                }
            }.start();

            try {
                String songFileName = currentSong.fileName(); // 수정: getSong() 대신 fileName() 호출
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            clearHints();
        }
    }

    private void clearHints() {
        // 힌트 초기화 작업을 수행합니다.
        // 힌트 관련 변수나 UI를 초기화하는 등의 작업을 진행하면 됩니다.
        // 예를 들어, 아래와 같이 힌트 텍스트뷰를 초기화할 수 있습니다.
        hintTextView.setText("");
    }

    private void nextQuestion() {
        // 이전 문제에 대한 처리
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();

        // 다음 문제로
        startGame();
    }

    private void checkAnswer() {
        if (currentSong == null) {
            return;
        }

        String userSong = songEditText.getText().toString().trim();
        String userArtist = artistEditText.getText().toString().trim();

        if (userSong.equalsIgnoreCase(currentSong.getSong())
                && userArtist.equalsIgnoreCase(currentSong.getSinger())) {
            Toast.makeText(this, "정답입니다!", Toast.LENGTH_SHORT).show();
            answeredQuestions++;
            remainingQuestions.remove(currentSong);

            if (timer != null) {
                timer.cancel();
            }

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();

            // 정답란 지우기
            songEditText.setText("");
            artistEditText.setText("");

            startGame();
        } else {
            Toast.makeText(this, "오답입니다!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHint(String hint) {
        hintTextView.setText("힌트: " + hint);
        hintTextView.setVisibility(View.VISIBLE);
    }


    private void finishGame() {
        Toast.makeText(this, "게임 종료!", Toast.LENGTH_SHORT).show();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
    }

    private List<Song> loadSongListFromCSV() {
        List<Song> songList = new ArrayList<>();

        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = getResources().openRawResource(R.raw.song);
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;

            // skip the first line (header row)
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 6) {
                    int number = Integer.parseInt(data[0]);
                    String song = data[1];
                    String singer = data[2];
                    String fileName = data[3];
                    String hint1 = data[4];
                    String hint2 = data[5];

                    Song newSong = new Song(number, song, singer, fileName, hint1, hint2);
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

        return songList;
    }


    private Song getRandomSong() {
        if (remainingQuestions.isEmpty()) {
            return null;
        }

        Random random = new Random();
        int randomIndex = random.nextInt(remainingQuestions.size());
        return remainingQuestions.get(randomIndex);
    }

   /* private void increaseVolume() {
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
    }

    private void decreaseVolume() {
        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
    }*/
}
