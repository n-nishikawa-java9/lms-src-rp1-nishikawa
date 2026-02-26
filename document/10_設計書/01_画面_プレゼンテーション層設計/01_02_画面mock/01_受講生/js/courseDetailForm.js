var workingDays = [];
var courseId;

var workingDayCount;

var numOfCategory = 0;
var amntOfSection = 0;

var numOfSection = 0;
var amntSection = new Map();

function getCategoryData() {

  $.ajax({
    type: "POST",
    url: "/lms/course/update/jsonCourseDto",
    data: "courseId=" + courseId,
    success: function(courseDto){

      var html = "";
      for (var i = 0; i < courseDto.categoryDtoList.length; i++) {

        html += '<div id="categoryArea' + numOfCategory + '" class="categoryArea well bs-component">'
        html += '<div class="mb20">';
        html += '<button type="button" class="btn btn-primary" onclick="addCategory(' + numOfCategory + ', 0);">上へカテゴリーを追加</button>';
        html += '&nbsp;<button type="button" class="btn btn-danger" onclick="delCategory(' + numOfCategory + ');">このカテゴリーを削除</button>';
        html += '</div>';
        html += '<fieldset>';

        html += '<div class="form-group">';
        html += '<label for="question_${s.index}" class="col-lg-2 control-label">カテゴリー名</label>';
        html += '<div class="col-lg-10">';
        html += '<input type="text" name="categoryName" value="' + courseDto.categoryDtoList[i].categoryName + '" class="form-control" maxlength="20"></div>';
        html += '<input type="hidden" name="categoryId" value="' + courseDto.categoryDtoList[i].categoryId + '">';
        html += '<input type="hidden" id="categoryDelFlg' + numOfCategory + '" name="categoryDelFlg" value="0">';
        html += '</div>';

        html += '<table id="categoryTable' + numOfCategory + '" class="table">';
        html += '<tr>';
        html += '<th>日付</th>';
        html += '<th>セクション名</th>';
        html += '<th></th>';
        html += '</tr>';

        for (var j = 0; j < courseDto.categoryDtoList[i].sectionDtoList.length; j++) {
          html += '<tr id="sectionRow' + numOfSection + '">';
          html += '<th class="date">' + formatDate(courseDto.categoryDtoList[i].sectionDtoList[j].date) + '</th>';
          html += '<td>';
          html += '<input type="hidden" name="sectionId" value="' + courseDto.categoryDtoList[i].sectionDtoList[j].sectionId + '" class="form-control">';
          html += '<input type="hidden" id="sectionDelFlg' + numOfSection + '" name="sectionDelFlg" class="sectionDelFlg" value="0">';
          html += '<input type="text" name="sectionName" value="' + courseDto.categoryDtoList[i].sectionDtoList[j].sectionName + '" class="form-control" maxlength="30">';
          html += '</td>';
          html += '<td>';
          html += '<button type="button" class="btn btn-primary" onclick="addSection(' + numOfCategory + ',' + numOfSection + ', 0);">上に追加</button>';
          html += '&nbsp;<button type="button" class="btn btn-primary" onclick="addSection(' + numOfCategory + ',' + numOfSection + ', 1);">下に追加</button>';
          html += '&nbsp;<button type="button" class="btn btn-danger" onclick="delSection('+ numOfCategory + ',' + numOfSection + ');">削除</button></td>';
          html += '</tr>';

          numOfSection++;
        }
        html += '</table>';
        html += '<button type="button" class="btn btn-primary" onclick="addCategory(' + numOfCategory + ', 1);">下へカテゴリーを追加</button>';
        html += '<input type="hidden" name="numOfSection" class="numOfSection" value="" />';
        html += '</div>';

        numOfCategory++;
        amntOfSection++;

      }

      $("#courseDetailArea").html(html);
      setWorkingDay();

    }

  });
}

function formatDate(time) {
  var date = new Date(time);
  var y = date.getFullYear();
  var m = date.getMonth() + 1;
  var d = date.getDate();
  return y + "/" + m + "/" + d;
}

function addCategoryInit() {

  var html = getAddCategoryHtml();

  $("#courseDetailArea").html(html);

  var amnt = amntSection.get(numOfCategory);
  if (amnt == null) {
    amnt = 0;
  } else {
    amnt++;
  }
  amntSection.set(numOfCategory, amnt);

  numOfCategory++;
  amntOfSection++;

  numOfSection++;

  setWorkingDay();
}

function addCategory(category, place) {

  var html = getAddCategoryHtml();

  var amnt = amntSection.get(numOfCategory);
  if (amnt == null) {
    amnt = 0;
  } else {
    amnt++;
  }
  amntSection.set(numOfCategory, amnt);

  if (place == 0) {
    $("#categoryArea" +
        category).before(html);
  } else if (place == 1) {
    $("#categoryArea" + category).after(html);
  }

  numOfCategory++;
  amntOfSection++;

  numOfSection++;

  setWorkingDay();
}

function getAddCategoryHtml() {
  var html = "";
  html += '<div id="categoryArea' + numOfCategory + '" class="categoryArea well bs-component">';
  html += '<div class="mb20">';
  html += '<button type="button" class="btn btn-primary" onclick="addCategory(' + numOfCategory + ', 0);">上へカテゴリーを追加</button>';
  html += '&nbsp;<button type="button" class="btn btn-danger" onclick="delCategory(' + numOfCategory + ');">このカテゴリーを削除</button>';
  html += '</div>';
  html += '<fieldset>';
  html += '<div class="form-group">';
  html += '<label for="question_${s.index}" class="col-lg-2 control-label">カテゴリー名</label>';
  html += '<div class="col-lg-10">';
  html += '<input type="text" name="categoryName" value="" class="form-control" maxlength="20"></div>';
  html += '<input type="hidden" name="categoryId" value="">';
  html += '<input type="hidden" id="categoryDelFlg' + numOfCategory + '" name="categoryDelFlg" value="0">';
  html += '</div>';
  html += '<table id="categoryTable' + numOfCategory + '" class="table">';
  html += '<tr>';
  html += '<th>日付</th>';
  html += '<th>セクション名</th>';
  html += '<th></th>';
  html += '</tr>';
  html += '<tr id="sectionRow' + numOfSection + '">';
  html += '<th class="date"></th>';
  html += '<td>';
  html += '<input type="hidden" name="sectionId" value="" class="form-control">';
  html += '<input type="hidden" id="sectionDelFlg' + numOfSection + '" name="sectionDelFlg" class="sectionDelFlg" value="0">';
  html += '<input type="text" name="sectionName" value="" class="form-control" maxlength="30">';
  html += '</td>';
  html += '<td>';
  html += '<button type="button" class="btn btn-primary" onclick="addSection('+ numOfCategory + ',' + numOfSection + ', 0);">上に追加</button>';
  html += '&nbsp;<button type="button" class="btn btn-primary" onclick="addSection('+ numOfCategory + ',' + numOfSection + ', 1);">下に追加</button>';
  html += '&nbsp;<button type="button" class="btn btn-danger" onclick="delSection('+ numOfCategory + ',' + numOfSection + ');">削除</button></td>';
  html += '</tr>';
  html += '</table>';
  html += '<button type="button" class="btn btn-primary" onclick="addCategory(' + numOfCategory + ', 1);">下へカテゴリーを追加</button>';
  html += '<input type="hidden" name="numOfSection" class="numOfSection" value="" />';
  html += '</div>';

  return html;
}

function addSection(category, section, place) {

  var html = "";

  html += '<tr id="sectionRow' + numOfSection + '">';
  html += '<th class="date"></th>';
  html += '<td>';
  html += '<input type="hidden" name="sectionId" value="" class="form-control">';
  html += '<input type="hidden" id="sectionDelFlg' + numOfSection + '" name="sectionDelFlg" class="sectionDelFlg" value="0">';
  html += '<input type="text" name="sectionName" value="" class="form-control" maxlength="30">';
  html += '</td>';
  html += '<td>';
  html += '<button type="button" class="btn btn-primary" onclick="addSection('+ category + ',' + numOfSection + ', 0);">上に追加</button>';
  html += '&nbsp;<button type="button" class="btn btn-primary" onclick="addSection('+ category + ',' + numOfSection + ', 1);">下に追加</button>';
  html += '&nbsp;<button type="button" class="btn btn-danger" onclick="delSection('+ category + ',' + numOfSection + ');">削除</button></td>';
  html += '</tr>';

  if (place == 0) {
    $("#sectionRow" + section).before(html);
  } else if (place == 1) {
    $("#sectionRow" + section).after(html);
  }

  numOfSection++;
  var amnt = amntSection.get(category);
  amnt++;
  amntSection.set(category, amnt);

  setWorkingDay();
}


function delCategory(category) {
  if (amntOfSection == 1) {
    alert('カテゴリーは最低一つ必要です。');
    return;
  }
  amntOfSection--;
  $("#categoryDelFlg" + category).val(1);
  $("#categoryArea" + category).each(function(i){
    $(this).find(".sectionDelFlg").val(1);
    $(this).find(".date").removeClass("date");
  });
  $("#categoryArea" + category).hide();
  setWorkingDay();
}


function delSection(category, section) {
    var amnt = amntSection.get(category);
    if (amnt == 0) {
      alert('カテゴリーには最低一つのセクションが必要です。');
      return;
    } else {
      amnt--;
    }
    amntSection.set(category, amnt);

  $("#sectionDelFlg" + section).val(1);
  $("#sectionRow" + section).find(".date").removeClass("date");
  $("#sectionRow" + section).hide();
  setWorkingDay();
}

function setWorkingDay() {
  $(".date").each(function(i){
    if (i < workingDays.length) {
      $(this).html(workingDays[i]);
    } else {
      $(this).html("コース期間超過");
    }
  });
}

function submitForm() {
  $(".categoryArea").each(function(i){
    $(this).find(".numOfSection").val($(this).find(".sectionDelFlg").length);
  });

  var requiredError = 0;
  for (var i = 0; i < numOfSection; i++) {

    if ($("#categoryDelFlg" + i).val() == 1) {
      continue;
    }
    $("#categoryArea" + i).each(function(i){
      if ($(this).find("input[name=categoryName]").val() == "") {
        requiredError++;
      }
    });

  }

  for (var i = 0; i < numOfSection; i++) {

    if ($("#sectionDelFlg" + i).val() == 1) {
      continue;
    }

    $("#sectionRow" + i).each(function(i){
      if ($(this).find("input[name=sectionName]").val() == "") {
        requiredError++;
      }
    });

  }

  if (requiredError > 0) {
    alert(requiredError + "個の未入力項目があります。");
    return false;
  }

  if ($(".date").size() != workingDays.length) {
    alert("日数が不一致です");
    return false;
  }

  $("#courseDetailForm").submit();

}