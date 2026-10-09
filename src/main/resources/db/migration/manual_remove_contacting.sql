-- Run on existing databases when deploying the removal of CONTACTING.
UPDATE leads SET status = CASE qualification_decision
    WHEN 'approved' THEN '已合格'
    WHEN 'rejected' THEN '不合格'
    ELSE '待聯繫' END
WHERE status = '聯繫中';
