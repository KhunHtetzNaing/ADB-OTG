package com.wandoujia.flashbot;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainActivity extends ActionBarActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction()
          .add(R.id.container, new PlaceholderFragment())
          .commit();
    }
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
    private Handler handler;

    private boolean rootSuccess = false;
    private TextView textView;
    private Button flashButton;

    private ImageView imageView;

    public PlaceholderFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.fragment_main, container, false);

      textView = (TextView) rootView.findViewById(R.id.textView);

      flashButton = (Button) rootView.findViewById(R.id.flashButton);
      flashButton.setEnabled(false);

      imageView = (ImageView) rootView.findViewById(R.id.imageView);


      handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
          Message state = Message.values()[msg.what];
          switch (state) {
            case DEVICE_FOUND:
              flashButton.setEnabled(true);
              flashButton.setBackgroundResource(R.drawable.button_ready);
              flashButton.setText(R.string.ready_flash_apks);
              break;

            case DEVICE_UNAUTHORIZED:
              flashButton.setEnabled(true);
              flashButton.setBackgroundResource(R.drawable.button_waiting);
              flashButton.setText(R.string.authorize_hint);
              break;

            case DEVICE_NOT_FOUND:
              flashButton.setEnabled(false);
              flashButton.setBackgroundResource(R.drawable.button_waiting);
              flashButton.setText(R.string.waiting_device);
              break;

            case FLASHING:
              flashButton.setEnabled(false);
              flashButton.setBackgroundResource(R.drawable.button_working);
              flashButton.setText(R.string.flashing_apks);
              break;
            case EXIT:
              new AlertDialog.Builder(getActivity()).setMessage("fail to get root shell, EXIT!").setCancelable(false).setPositiveButton("exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  getActivity().finish();
                  System.exit(0);
                }
              }).show();
              break;

          }
        }
      };

      Shell shell = ShellUtils.getRootShell();
      rootSuccess = (shell != null);

      if (!rootSuccess) {
        handler.sendEmptyMessage(Message.EXIT.ordinal());
      }

      if (rootSuccess) {
        try {
          restartAdb();
        } catch (IOException e) {
          e.printStackTrace();
        }
        handler.postDelayed(new AdbDevicesMonitor(handler), 1000);
      }

      flashButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          try {
            MessageHolder.message = Message.FLASHING;
            handler.sendEmptyMessage(Message.FLASHING.ordinal());

            File dir = new File("/sdcard/Flashbot/apks");
            if (!dir.exists()) {
              dir.mkdirs();
            }

            List<File> files = new ArrayList<File>();
            files.addAll(Arrays.asList(dir.listFiles()));
            Collections.sort(files, new Comparator<File>() {
              @Override
              public int compare(File lhs, File rhs) {
                return lhs.getName().compareTo(rhs.getName());
              }
            });

            final int total = files.size();

            Iterator<File> iterator = files.iterator();

            if (iterator.hasNext()) {
              File file = iterator.next();

              Command command = new SequenceAdbInstallCommandCapture(file, iterator, 1, total);
              ShellUtils.getRootShell().add(command);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }


      });

      return rootView;
    }

    private void restartAdb() throws IOException {
      ShellUtils.getRootShell().add(new CommandCapture(IdGenerator.next(), Const.timeout, "adb kill-server") {
        @Override
        public void commandCompleted(int id, int exitcode) {
          super.commandCompleted(id, exitcode);
          final List<String> pids = new ArrayList<String>();
          try {
            ShellUtils.getRootShell().add(new CommandCapture(IdGenerator.next(), "ps") {
              @Override
              public void commandOutput(int id, String line) {
                super.commandOutput(id, line);

                String[] tokens = line.split("\t");

                if (tokens.length != 8)
                  return;

                if (tokens[8].equalsIgnoreCase("adb")) {
                  pids.add(tokens[1]);
                }
              }

              @Override
              public void commandTerminated(int id, String reason) {
              }

              @Override
              public void commandCompleted(int id, int exitcode) {
                super.commandCompleted(id, exitcode);
                for (String pid : pids) {
                  try {
                    ShellUtils.getRootShell().add(new CommandCapture(IdGenerator.next(), "kill -9 " + pid));
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }

              }
            });
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    }

    private class SequenceAdbInstallCommandCapture extends CommandCapture {

      private Iterator<File> iterator;

      private File file;

      private int offset;

      private int total;

      public SequenceAdbInstallCommandCapture(final File file, Iterator<File> iterator, final int offset, final int total) {
        super(IdGenerator.next(), Const.timeout, "adb install -r " + "/sdcard/Flashbot/apks/" + file.getName());
        this.file = file;
        this.iterator = iterator;

        this.offset = offset;
        this.total = total;


        handler.post(new Runnable() {
          @Override
          public void run() {
            textView.setText(offset + "/" + total);

            try {
              Drawable image = IconUtils.getApkIcon(getActivity().getApplicationContext(), file.getAbsolutePath());

              imageView.setVisibility(View.VISIBLE);
              imageView.setImageDrawable(image);
            } catch (Exception e) {
              Log.e(Const.TAG, "extract icon from " + file + " failed", e);
            }
          }
        });

        Log.i(Const.TAG, "start installing " + file.getName());

      }

      @Override
      public void commandOutput(int id, final String line) {
        super.commandOutput(id, line);
        Log.i(Const.TAG, line);
      }

      @Override
      public void commandTerminated(int id, String reason) {
        super.commandTerminated(id, reason);
        Log.e(Const.TAG, file.getName() + " installation fails because " + reason);
        if (iterator.hasNext()) {
          File nextFile = iterator.next();

          try {
            ShellUtils.getRootShell().add(new SequenceAdbInstallCommandCapture(nextFile, iterator, offset + 1, total));
          } catch (IOException e) {
            Log.e(Const.TAG, e.getMessage(), e);
          }
        } else {
          startPhoenix();
          imageView.setVisibility(View.INVISIBLE);
          Log.i(Const.TAG, "no more apks after termination");
          MessageHolder.message = Message.DEVICE_FOUND;
          handler.sendEmptyMessage(Message.DEVICE_FOUND.ordinal());
        }
      }

      @Override
      public void commandCompleted(int id, int exitcode) {
        super.commandCompleted(id, exitcode);
        Log.i(Const.TAG, file.getName() + " installation success");

        if (iterator.hasNext()) {
          File nextFile = iterator.next();

          try {
            ShellUtils.getRootShell().add(new SequenceAdbInstallCommandCapture(nextFile, iterator, offset + 1, total));
          } catch (IOException e) {
            Log.e(Const.TAG, e.getMessage(), e);
          }
        } else {
          startPhoenix();
          imageView.setVisibility(View.INVISIBLE);
          Log.i(Const.TAG, "no more apks after completion");

          MessageHolder.message = Message.DEVICE_FOUND;
          handler.sendEmptyMessage(Message.DEVICE_FOUND.ordinal());
        }
      }

      private void startPhoenix() {
        try {
          ShellUtils.getRootShell().add(new CommandCapture(IdGenerator.next(), "adb shell am start -n com.wandoujia.phoenix2/com.wandoujia.phoenix2.NewWelcomeActivity") {
            @Override
            public void commandTerminated(int id, final String reason) {
              super.commandTerminated(id, reason);

              Log.e(Const.TAG, "open phoenix failed because " + reason);
            }

            @Override
            public void commandCompleted(int id, final int exitcode) {
              super.commandCompleted(id, exitcode);
              Log.i(Const.TAG, "open phoenix return " + exitcode);
            }
          });
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

}
