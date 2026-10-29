package jp.co.sss.lms.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private TrainingTime trainingTime;
	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
		//西川直希 - Task.26
		attendanceForm.setHourMap(attendanceUtil.getHourMap());
		attendanceForm.setMinuteMap(attendanceUtil.getMinuteMap());
		//Task.26 - ここまで

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			dailyAttendanceForm
					.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			//西川直希 - Task.26
			//勤怠の出勤時間がある場合、時間を時、分に分割して差し替え
			if (attendanceManagementDto.getTrainingStartTime() != null
					&& !attendanceManagementDto.getTrainingStartTime().isBlank()) {
				dailyAttendanceForm
						.setTrainingStartTimeHour(
								attendanceUtil.getHour(attendanceManagementDto.getTrainingStartTime()));
				dailyAttendanceForm
						.setTrainingStartTimeMinute(
								attendanceUtil.getMinute(attendanceManagementDto.getTrainingStartTime()));
			}
			//勤怠の退勤時間がある場合、時間を時、分に分割して差し替え
			if (attendanceManagementDto.getTrainingEndTime() != null
					&& !attendanceManagementDto.getTrainingEndTime().isBlank()) {
				dailyAttendanceForm
						.setTrainingEndTimeHour(attendanceUtil.getHour(attendanceManagementDto.getTrainingEndTime()));
				dailyAttendanceForm
						.setTrainingEndTimeMinute(
								attendanceUtil.getMinute(attendanceManagementDto.getTrainingEndTime()));
			}

			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			// 日次勤怠フォームから更新用のエンティティにコピー
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			tStudentAttendance
					.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			// 出勤時刻整形
			TrainingTime trainingStartTime = null;
			trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
			tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			// 退勤時刻整形
			TrainingTime trainingEndTime = null;
			trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
			tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠未入力チェック
	 * @author 西川直希 - Task.25
	 * @return 未入力チェック結果
	 * @throws ParseException
	 */
	public Boolean notEnterCheck() throws ParseException {

		//現在の日付を取得
		Date todayDate = attendanceUtil.getTrainingDate();

		//未入力件数取得
		Integer count = tStudentAttendanceMapper.notEnterCount(
				loginUserDto.getLmsUserId(),
				Constants.DB_FLG_FALSE,
				todayDate);

		return count > 0;

	}

	/**
	 * 入力された出退勤の{時間}{分}をhh:mm形式に変換
	 * @author 西川直希 - Task.26
	 * @param attendanceForm
	 */
	public void formatConversion(AttendanceForm attendanceForm) {

		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			//出勤時間の取得
			Integer startHour = dailyAttendanceForm.getTrainingStartTimeHour();
			Integer startMinute = dailyAttendanceForm.getTrainingStartTimeMinute();

			//退勤時間の時・分が両方入力されている場合に時・分を結合
			if (startHour != null && startMinute != null) {
				dailyAttendanceForm.setTrainingStartTime(String.format("%02d:%02d", startHour, startMinute));
			}

			//出勤時間の取得
			Integer endHour = dailyAttendanceForm.getTrainingEndTimeHour();
			Integer endMinute = dailyAttendanceForm.getTrainingEndTimeMinute();

			//退勤時間の時・分が両方入力されている場合に時・分を結合
			if (endHour != null && endMinute != null) {
				dailyAttendanceForm.setTrainingEndTime(String.format("%02d:%02d", endHour, endMinute));
			}
		}
	}

	/**
	 * 入力チェック
	 * @author 西川直希 - Task.27
	 * @param attendanceForm
	 * @param result
	 */
	public void updateInputCheck(AttendanceForm attendanceForm, BindingResult result) {

		for (int i = 0; i < attendanceForm.getAttendanceList().size(); i++) {

			DailyAttendanceForm attendance = attendanceForm.getAttendanceList().get(i);
			Integer startHour = attendance.getTrainingStartTimeHour();
			Integer startMinute = attendance.getTrainingStartTimeMinute();
			Integer endHour = attendance.getTrainingEndTimeHour();
			Integer endMinute = attendance.getTrainingEndTimeMinute();
			String trainingStartTime = attendance.getTrainingStartTime();
			String trainingEndTime = attendance.getTrainingEndTime();

			//備考欄の文字数が100文字異常の時エラーメッセージを追加設定
			if (!StringUtils.isBlank(attendance.getNote()) && attendance.getNote().length() > 100) {

				result.addError(new FieldError(result.getObjectName(),
						"attendanceList[" + i + "].note",
						messageUtil.getMessage(Constants.VALID_KEY_MAXLENGTH,
								new String[] { "備考", "100" })));
			}

			//出勤時間の時が未入力のときにエラーメッセージを追加設定
			if (startHour == null && startMinute != null) {

				result.addError(new FieldError(result.getObjectName(),
						"attendanceList[" + i + "].trainingStartTimeHour",
						messageUtil.getMessage(Constants.INPUT_INVALID,
								new String[] { "出勤時間" })));
			}

			//出勤時間の分が未入力のときにエラーメッセージを追加設定
			if (startHour != null && startMinute == null) {
				result.addError(new FieldError(result.getObjectName(),
						"attendanceList[" + i + "].trainingStartTimeMinute",
						messageUtil.getMessage(Constants.INPUT_INVALID,
								new String[] { "出勤時間" })));
			}

			//退勤時間の時が未入力のときにエラーメッセージを追加設定
			if (endHour == null && endMinute != null) {

				result.addError(new FieldError(result.getObjectName(),
						"attendanceList[" + i + "].trainingEndTimeHour",
						messageUtil.getMessage(Constants.INPUT_INVALID,
								new String[] { "退勤時間" })));
			}

			//退勤時間の分が未入力のときにエラーメッセージを追加設定
			if (endHour != null && endMinute == null) {
				result.addError(new FieldError(result.getObjectName(),
						"attendanceList[" + i + "].trainingEndTimeMinute",
						messageUtil.getMessage(Constants.INPUT_INVALID,
								new String[] { "退勤時間" })));
			}

			//出勤時間が未入力で退勤時間が入力されているときにエラーメッセージを追加設定
			if (startHour == null && startMinute == null && endHour != null && endMinute != null) {

				result.addError(new FieldError(result.getObjectName(),
						"attendanceList[" + i + "].trainingStartTime",
						messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY)));
			}

			//エラーがあるかどうかチェック
			if (result.hasErrors()) {
				continue;
			}

			TrainingTime trainingstartTime = new TrainingTime(trainingStartTime);
			TrainingTime trainingendTime = new TrainingTime(trainingEndTime);

			//出勤時刻 ＞ 退勤時刻 になっていないか比較チェック
			if (trainingstartTime.compareTo(trainingendTime) > 0) {
				result.addError(new FieldError(
						result.getObjectName(),
						"attendanceList[" + i + "].trainingStartTime",
						messageUtil.getMessage(
								Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE,
								new String[] { String.valueOf(i) })));
				continue;
			}

			//中抜け時間が入力されているとき
			if (attendance.getBlankTime() != null) {
				TrainingTime startTime = new TrainingTime(0, 0);
				TrainingTime endTime = new TrainingTime(0, 0);

				//出退勤時間が入力されているとき
				if (!StringUtils.isBlank(attendance.getTrainingStartTime())
						&& !StringUtils.isBlank(attendance.getTrainingEndTime())) {
					//出退勤時間が適正であるとき
					if (trainingTime.isValidTrainingTime(trainingStartTime)
							&& trainingTime.isValidTrainingTime(trainingEndTime)) {
						startTime = new TrainingTime(trainingStartTime);
						endTime = new TrainingTime(trainingEndTime);
					}
				}
				//受講時間を取得
				TrainingTime jukoTime = attendanceUtil.calcJukoTime(startTime, endTime);
				Integer jukoMinutes = attendanceUtil.reverseBlankTime(jukoTime.toString());

				//中抜け時間 ＞ (退勤時間 - 出勤時間)になっていないかチェック
				if (attendance.getBlankTime() > jukoMinutes) {
					result.addError(new FieldError(result.getObjectName(),
							"attendanceList[" + i + "].blankTime",
							messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_BLANKTIMEERROR)));
				}

			}

		}

	}

}
