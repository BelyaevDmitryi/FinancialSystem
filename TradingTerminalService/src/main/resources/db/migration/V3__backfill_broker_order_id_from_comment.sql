-- Backfill broker_order_id from legacy comment prefix (US-OSE-001 task 15)
UPDATE orders
SET broker_order_id = split_part(
    trim(substring(comment FROM strpos(comment, 'Broker Order ID: ') + char_length('Broker Order ID: '))),
    ' ',
    1
)
WHERE (broker_order_id IS NULL OR btrim(broker_order_id) = '')
  AND comment IS NOT NULL
  AND btrim(comment) <> ''
  AND strpos(comment, 'Broker Order ID: ') > 0;
