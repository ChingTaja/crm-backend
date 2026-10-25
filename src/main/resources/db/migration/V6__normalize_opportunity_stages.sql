UPDATE opportunities SET stage = CASE
    WHEN stage IN ('需求確認', '提案報價', '協商中') THEN '需求討論中'
    WHEN stage = '已成交' THEN '需求成交'
    WHEN stage = '已失單' THEN '失單'
    ELSE stage END
WHERE stage IN ('需求確認', '提案報價', '協商中', '已成交', '已失單');

