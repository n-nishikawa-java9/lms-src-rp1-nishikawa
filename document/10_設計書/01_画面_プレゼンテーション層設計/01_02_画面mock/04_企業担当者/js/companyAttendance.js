$(function(){
  /**
   * ダウンロードボタンを使用できなくする
   */
  function disableDownloadButton() {
    $('#downloadButton').hide();
    // ダウンロード出来ない代わりに、押すとアラートを出力するボタンを表示
    $('#downloadAlertButton').show();
  }
  /**
   * 勤怠フォームのコピーを実施
   */
  $('#copyAttendance').click(function() {
    disableDownloadButton();
    var startTimeAry = $('.startTime');
    var endTimeAry = $('.endTime');
    var startTimeRoundedAry = $('.startTimeRounded');
    var endTimeRoundedAry = $('.endTimeRounded');
    var idxI, $startTime, $endTime, $startTimeRounded, $endTimeRounded;
    for (idxI = 0; idxI < startTimeAry.length; idxI++) {
      $startTime = $(startTimeAry[idxI]);
      $endTime = $(endTimeAry[idxI]);
      $startTimeRounded = $(startTimeRoundedAry[idxI]);
      $endTimeRounded = $(endTimeRoundedAry[idxI]);

      if ($startTime.val() === '' && !($startTimeRounded.val() === '')) {
          $startTime.val($startTimeRounded.val());
          $startTime.addClass('warnInput');
      }
      if ($endTime.val() === '' && !($endTimeRounded.val() === '')) {
          $endTime.val($endTimeRounded.val());
          $endTime.addClass('warnInput');
      }
    }
  });
  $('#downloadAlertButton').click(function() {
    alert('勤怠情報が修正されているためダウンロード出来ません。\n修正内容を一度保存してからダウンロードしてください。');
  });
  /**
   * 勤怠の値が変更されたら、ダウンロードを出来ないようにする
   */
  $('.attendanceFormControl').change(disableDownloadButton);

  /**
   * 勤怠一括登録フォームのコピーを実施（コピーは日別毎）
  */
  $(document).on('click', '#copyAttendanceDaily', function(){
    var index = $(this).val();
    var startTimeCompanyAry = $('.startTimeCompany' + index);
    var endTimeCompanyAry = $('.endTimeCompany'  + index);
    var startTimeCompanyRoundedAry = $('.startTimeCompanyRounded' + index);
    var endTimeCompanyRoundedAry = $('.endTimeCompanyRounded' + index);
    var idxI, $startTimeCompany, $endTimeCompany, $startTimeCompanyRounded, $endTimeCompanyRounded;
    for (idxI = 0; idxI < startTimeCompanyAry.length; idxI++) {
      $startTimeCompany = $(startTimeCompanyAry[idxI]);
      $endTimeCompany = $(endTimeCompanyAry[idxI]);
      $startTimeCompanyRounded = $(startTimeCompanyRoundedAry[idxI]);
      $endTimeCompanyRounded = $(endTimeCompanyRoundedAry[idxI]);
      if ($startTimeCompany.val() === '' && !($startTimeCompanyRounded.val() === '')) {
        $startTimeCompany.val($startTimeCompanyRounded.val());
        $startTimeCompany.addClass('warnInput');
      }
      if ($endTimeCompany.val() === '' && !($endTimeCompanyRounded.val() === '')) {
        $endTimeCompany.val($endTimeCompanyRounded.val());
        $endTimeCompany.addClass('warnInput');
      }
    }
  });
});

