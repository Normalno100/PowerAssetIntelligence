from zipfile import ZipFile, ZIP_DEFLATED
from pathlib import Path
from xml.sax.saxutils import escape

OUT = Path('docs/presentations/rosseti_ai_asset_platform_pitch.pptx')
OUT.parent.mkdir(parents=True, exist_ok=True)

W,H=12192000,6858000

def tx(text,x,y,w,h,size=2400,bold=False,color='FFFFFF'):
    paras=''.join(f'<a:p><a:r><a:rPr lang="ru-RU" sz="{size}"{(" b=\"1\"" if bold else "")}><a:solidFill><a:srgbClr val="{color}"/></a:solidFill></a:rPr><a:t>{escape(line)}</a:t></a:r><a:endParaRPr lang="ru-RU" sz="{size}"/></a:p>' for line in text.split('\n'))
    return f'''<p:sp><p:nvSpPr><p:cNvPr id="{tx.i}" name="Text {tx.i}"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:noFill/><a:ln><a:noFill/></a:ln></p:spPr><p:txBody><a:bodyPr wrap="square"/><a:lstStyle/>{paras}</p:txBody></p:sp>'''
tx.i=10

def rect(x,y,w,h,fill='102A43',line='2DD4BF',alpha=None):
    tx.i+=1
    af=f'<a:alpha val="{alpha}"/>' if alpha else ''
    return f'''<p:sp><p:nvSpPr><p:cNvPr id="{tx.i}" name="Box {tx.i}"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm><a:prstGeom prst="roundRect"><a:avLst/></a:prstGeom><a:solidFill><a:srgbClr val="{fill}">{af}</a:srgbClr></a:solidFill><a:ln w="19050"><a:solidFill><a:srgbClr val="{line}"/></a:solidFill></a:ln></p:spPr></p:sp>'''

def slide_xml(title, subtitle='', bullets=None, footer='Power Asset Intelligence × Россети | конкурсная презентация', accent='22D3EE', body_shapes=''):
    tx.i=20
    bullets=bullets or []
    bg=f'<p:bg><p:bgPr><a:solidFill><a:srgbClr val="061826"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>'
    logo=rect(330000,220000,700000,330000,'0EA5E9','67E8F9')+tx('PAI',455000,285000,430000,150000,1400,True,'FFFFFF')
    head=tx(title,520000,760000,6200000,650000,3100,True,'FFFFFF')
    sub=tx(subtitle,560000,1370000,6500000,420000,1500,False,'A7F3D0') if subtitle else ''
    # right decorative grid
    deco=''.join(rect(9000000+i*520000,600000+j*410000,360000,240000, ['0EA5E9','22C55E','F59E0B','EF4444'][(i+j)%4], '164E63', '35000') for i in range(4) for j in range(3))
    bxml=''
    y=1900000
    for b in bullets:
        bxml += rect(650000,y-70000,7600000,430000,'0B2537','155E75')
        bxml += tx('• '+b,820000,y+25000,7200000,240000,1500,False,'E0F2FE')
        y+=560000
    foot=tx(footer,520000,6420000,7000000,220000,900,False,'94A3B8')
    num=tx(str(slide_xml.num),11200000,6420000,500000,220000,900,True,'38BDF8')
    slide_xml.num += 1
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:cSld>{bg}<p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{W}" cy="{H}"/><a:chOff x="0" y="0"/><a:chExt cx="{W}" cy="{H}"/></a:xfrm></p:grpSpPr>{logo}{deco}{head}{sub}{bxml}{body_shapes}{foot}{num}</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>'''
slide_xml.num=1

slides=[
    ('AI‑платформа интеллектуального управления активами электрических сетей','Predictive Asset Intelligence for reliability, safety and lower OPEX',[]),
    ('Резюме проекта','Продукт для сетевой компании: риск → объяснение → действие',['Единый цифровой профиль актива: паспорт, телеметрия, ремонты, риск','AI рассчитывает вероятность отказа и приоритет обслуживания','MVP: backend, dashboard, Kafka, ML‑service, observability','Пилот: 100–300 активов, 12 недель, подтверждение эффектов']),
    ('Решаемая проблема','Реактивный ремонт дороже предиктивного управления',['Данные разрознены: SCADA/IoT, ремонты, паспорта, инциденты','Текущий подход: регламент + ручная экспертиза + реакция на аварии','Нехватка explainable AI: инженеру нужны причины и рекомендации','Эффекты проблемы: аварийность, внеплановые выезды, SAIDI/SAIFI, OPEX']),
    ('Описание решения','Decision Intelligence layer для ДЗО Россети',['Собираем данные по активам и потоковую телеметрию','Считаем risk score / risk level для каждого актива','Показываем причины: перегрев, нагрузка, вибрация, возраст, история отказов','Формируем очередь обслуживания и next best action']),
    ('Как решение применяется','От события телеметрии до ремонтного решения',['1. Телеметрия поступает через API/Kafka','2. AI обновляет риск и объяснение','3. Диспетчер видит top risky assets и heatmap','4. Инженер подтверждает рекомендацию и запускает ТО']),
    ('Технология / суть инновации','Explainable Predictive Maintenance Platform',['Гибридный AI: правила + ML‑prediction + anomaly detection','Clean Architecture: AI заменяем без переписывания use cases','Отдельный FastAPI ML‑service: failure_probability, risk_score, risk_level','Auditability: каждое решение воспроизводимо и объяснимо']),
    ('Конкурентные преимущества','Почему именно наше решение',['Не только мониторинг, а полный цикл: данные → риск → рекомендация','Explainable AI для инженерного доверия и аудита','Streaming-ready: Kafka для телеметрии вблизи реального времени','Отечественный open-source stack: Java, Python, PostgreSQL, Kafka, Grafana']),
    ('Бизнес‑модель и рынок','SaaS / on‑premise pilot → industrial rollout',['Коммерческое предложение: лицензия ПО + внедрение + поддержка','Ценообразование: число активов, источники данных, SLA, доработки интеграций','Заказчики: ДЗО Россети, ТСО, промышленные сети, генерирующие компании','Потенциал: масштабирование на трансформаторы, линии, выключатели, подстанции']),
    ('Статус проекта','MVP готов к демонстрации и пилотной адаптации',['Реализованы API активов, телеметрии, ремонтов и risk analysis','Есть dashboard: asset registry, telemetry trend, risk command center','Подготовлен ML‑service для прогнозирования отказов','Production stack: PostgreSQL, Kafka, Prometheus, Loki, Grafana']),
    ('Предложение заказчику на пилот','Пилот 12 недель на референтном объекте ДЗО',['Объект: 100–300 критичных активов 6–110 кВ','Функционал: сбор данных, risk scoring, explainability, top‑risk queue','Интеграции: выгрузки SCADA/АСУ ТП/ЕАМ или CSV/API на первом этапе','Соответствие: AI, цифровые решения, импортозамещение, надежность сетей']),
    ('Затраты и эффекты пилота','Проверяем экономику на измеримых KPI',['Оценка пилота: 8–15 млн ₽, включая адаптацию и интеграции','Целевые эффекты: −10–20% внеплановых выездов, −15–30% времени анализа инцидента','Метрики: SAIDI/SAIFI, аварийные ремонты, трудозатраты, точность risk ranking','После пилота: лицензия + сопровождение + расширение на портфель активов']),
    ('Команда проекта','Кросс‑функциональная команда под пилот',['Product / TPM: постановка ценности, пилот, KPI и заказчик','Solution architect: интеграции, security, reliability, architecture','Backend / data engineer: API, Kafka, PostgreSQL, ETL','ML engineer: признаки, модели, explainability, мониторинг качества','Industry expert: валидация правил риска и ремонтных сценариев']),
    ('Заключение','Пилот, который быстро проверяет ценность для сетевой компании',['Внедряем: AI‑платформу risk‑based управления активами','Где: пилотный контур ДЗО на 100–300 критичных активах','Что доказываем: объяснимый риск, приоритизация ТО, измеримый эффект','Первые шаги: выбрать объект → согласовать данные → запустить пилот → оценить KPI']),
    ('Контакты','Power Asset Intelligence',['Компания: Power Asset Intelligence','Контактное лицо: technical product manager / руководитель пилота','E-mail: demo@powerasset.ai','Телефон: +7 XXX XXX‑XX‑XX','Формат следующего шага: 60‑минутная рабочая встреча по объекту пилота']),
]

content_types='''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>''' + ''.join(f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>' for i in range(1,len(slides)+1)) + '<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/></Types>'
root_rels='''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>'''
pres='''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:sldIdLst>''' + ''.join(f'<p:sldId id="{255+i}" r:id="rId{i}"/>' for i in range(1,len(slides)+1)) + '</p:sldIdLst><p:sldSz cx="12192000" cy="6858000" type="wide"/><p:notesSz cx="6858000" cy="9144000"/><p:defaultTextStyle/></p:presentation>'
pres_rels='''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">''' + ''.join(f'<Relationship Id="rId{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>' for i in range(1,len(slides)+1)) + '</Relationships>'
core='''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>AI-платформа интеллектуального управления активами электрических сетей</dc:title><dc:creator>Power Asset Intelligence</dc:creator><cp:lastModifiedBy>OpenAI GPT-5.5</cp:lastModifiedBy></cp:coreProperties>'''
app=f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>PowerPoint</Application><PresentationFormat>Широкоэкранный</PresentationFormat><Slides>{len(slides)}</Slides></Properties>'''

with ZipFile(OUT,'w',ZIP_DEFLATED) as z:
    z.writestr('[Content_Types].xml', content_types)
    z.writestr('_rels/.rels', root_rels)
    z.writestr('ppt/presentation.xml', pres)
    z.writestr('ppt/_rels/presentation.xml.rels', pres_rels)
    z.writestr('docProps/core.xml', core)
    z.writestr('docProps/app.xml', app)
    for i,(t,s,b) in enumerate(slides,1):
        z.writestr(f'ppt/slides/slide{i}.xml', slide_xml(t,s,b))
        z.writestr(f'ppt/slides/_rels/slide{i}.xml.rels','''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>''')
print(OUT)
