//////////////////////////////////////////////////////////////////////////////
///
/// C# example that manipulates mp3 audio files with SoundTouch library.
/// 
/// Author        : Copyright (c) Olli Parviainen
/// Author e-mail : oparviai 'at' iki.fi
/// SoundTouch WWW: http://www.surina.net/soundtouch
///
////////////////////////////////////////////////////////////////////////////////
//
// License for this source code file: Microsoft Public License(Ms-PL)
//
////////////////////////////////////////////////////////////////////////////////

using soundtouch;
using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace csharp_example
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        protected SoundProcessor processor = new SoundProcessor();

        public MainWindow()
        {
            InitializeComponent();

            StatusMessage.statusEvent += StatusEventHandler;
            processor.PlaybackStopped += EventHandler_playbackStopped;
            DisplaySoundTouchVersion();
        }


        /// <summary>
        /// Display SoundTouch library version string in status bar. This also indicates whether the DLL was loaded succesfully or not ...
        /// </summary>
        private void DisplaySoundTouchVersion()
        {
            string status;
            try
            {
                status = String.Format("SoundTouch version: {0}", SoundTouch.GetVersionString());
            }
            catch (Exception exp)
            {
                status = exp.Message;
            }
            text_status.Text = status;
        }


        private void StatusEventHandler(object sender, string msg)
        {
            text_status.Text = msg;
        }


        private void button_browse_Click(object sender, RoutedEventArgs e)
        {
            Microsoft.Win32.OpenFileDialog openDialog = new Microsoft.Win32.OpenFileDialog();
            openDialog.Filter = "MP3 files (*.mp3)|*.mp3";
            if (openDialog.ShowDialog() == true)
            {
                if (processor.OpenMp3File(openDialog.FileName) == true)
                {
                    textBox_filename.Text = openDialog.FileName;
                    button_play.IsEnabled = true;
                    button_stop.IsEnabled = true;

                    // Parse adjustment settings
                    ParseTempoTextBox();
                    ParsePitchTextBox();
                    ParseRateTextBox();
                }
                else
                {
                    MessageBox.Show("Coudln't open audio file " + openDialog.FileName);
                }
            }
        }


        private void setPlayButtonMode(bool play)
        {
            button_play.Content = play ? "_Play" : "_Pause";
        }


        private void EventHandler_playbackStopped(object sender, bool hasReachedEnd)
        {
            if (hasReachedEnd)
            {
                text_status.Text = "Stopped";
            }   // otherwise paused

            setPlayButtonMode(true);
        }


        private void button_play_Click(object sender, RoutedEventArgs e)
        {
            if (button_play.Content == "_Pause")
            {
                // Pause
                if (processor.Pause())
                {
                    text_status.Text = "Paused";
                }
                setPlayButtonMode(true);
            }
            else
            {
                // Play
                if (processor.Play())
                {
                    text_status.Text = "Playing";
                    setPlayButtonMode(false);
                }
            }
        }


        private void button_stop_Click(object sender, RoutedEventArgs e)
        {
            if (processor.Stop())
            {
                text_status.Text = "Stopped";
            }
            setPlayButtonMode(true);
        }


        private bool parse_percentValue(TextBox box, out double value)
        {
            if (double.TryParse(box.Text, out value) == false) return false;
            if (value < -99.0) value = -99.0;   // don't allow more than -100% slowdown ... :)
            box.Text = value.ToString();
            return true;
        }


        private void ParsePitchTextBox()
        {
            double pitchValue;
            if (double.TryParse(textBox_pitch.Text, out pitchValue))
            {
                if (processor.streamProcessor != null) processor.streamProcessor.st.SetPitchSemiTones((float)pitchValue);
            }
        }


        private void ParseTempoTextBox()
        {
            double tempoValue;
            if (parse_percentValue(textBox_tempo, out tempoValue))
            {
                if (processor.streamProcessor != null) processor.streamProcessor.st.SetTempoChange((float)tempoValue);
            }
        }


        private void ParseRateTextBox()
        {
            double rateValue;
            if (parse_percentValue(textBox_rate, out rateValue))
            {
                if (processor.streamProcessor != null) processor.streamProcessor.st.SetRateChange((float)rateValue);
            }
        }


        private void textBox_tempo_LostFocus(object sender, RoutedEventArgs e)
        {
            ParseTempoTextBox();
        }


        private void textBox_tempo_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter)
            {
                // enter pressed -- parse value
                ParseTempoTextBox();
            }
        }


        private void textBox_pitch_LostFocus(object sender, RoutedEventArgs e)
        {
            ParsePitchTextBox();
        }


        private void textBox_pitch_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter)
            {
                // enter pressed -- parse value
                ParsePitchTextBox();
            }
        }


        private void textBox_rate_LostFocus(object sender, RoutedEventArgs e)
        {
            ParseRateTextBox();
        }


        private void textBox_rate_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter)
            {
                // enter pressed -- parse value
                ParseRateTextBox();
            }
        }
    }
}
