$ ->
  # set language
  if localStorage.getItem 'language'
    language = (String) localStorage.getItem 'language'
  else
    language = 'de'   # default language
    localStorage.setItem 'language', language

  # set search type
  if localStorage.getItem 'search-type'
    search_type = (Number) localStorage.getItem 'search-type'
  else
    search_type = 1   # default search type is 'article'

  setSearchQuery = (lang, type) ->
    if type == 1
      return '/name?lang=' + lang + '&name='
    else if type == 2
      return '/owner?lang=' + lang + '&owner='
    else if type == 3
      return '/atc?lang=' + lang + '&atc='
    else if type == 4
      return '/regnr?lang=' + lang + '&regnr='
    else if type == 5
      return '/therapy?lang=' + lang + '&therapy='
    return '/name?lang=' + lang + '&name='

  # default value
  search_query = setSearchQuery(language, 1)

  start_time = new Date().getTime()

  typed_input = ''

  articles = new Bloodhound(
    datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name')
    queryTokenizer: Bloodhound.tokenizers.whitespace
    remote:
      wildcard: '%QUERY'
      url: search_query
      replace: (url, query) ->
        return search_query + query
      filter: (list) ->
        document.getElementById('num-results').textContent=list.length
        return list
  )

  # kicks off the loading/processing of "remote" and "prefetch"
  articles.initialize()

  typeaheadCtrl = $('#input-form .twitter-typeahead')

  typeaheadCtrl.typeahead
    menu: $('#special-dropdown')
    hint: false
    highlight: false
    minLength: 1
  ,
    name: 'articles'
    displayKey: 'name'
    limit: '40'
    # "ttAdapter" wraps the suggestion engine in an adapter that is compatible with the typeahead jQuery plugin
    source: articles.ttAdapter()
    templates:
      suggestion: (data) ->
        if search_type == 1
          "<div style='display:table;vertical-align:middle;'>\
          <p style='color:#444444;font-size:1.0em;'><b>#{data.title}</b></p>\
          <span style='font-size:0.85em;'>#{data.packinfo}</span></div>"
        else if search_type == 2
          "<div style='display:table;vertical-align:middle;'>\
          <p style='color:#444444;font-size:1.0em;'><b>#{data.title}</b></p>\
          <span style='color:#8888cc;font-size:1.0em;'><p>#{data.author}</p></span></div>"
        else if search_type == 3
          "<div style='display:table;vertical-align:middle;'>\
          <p style='color:#444444;font-size:1.0em;'><b>#{data.title}</b></p>\
          <span style='color:gray;font-size:0.85em;'>#{data.atccode}</span></div>"
        else if search_type == 4
          "<div style='display:table;vertical-align:middle;'>\
          <p style='color:#444444;font-size:1.0em;'><b>#{data.title}</b></p>\
          <span style='color:#8888cc;font-size:1.0em;'><p>#{data.regnrs}</p></span></div>"
        else if search_type == 5
          "<div style='display:table;vertical-align:middle;'>\
          <p style='color:#444444;font-size:1.0em;'><b>#{data.title}</b></p>\
          <span style='color:gray;font-size:0.85em;'>#{data.therapy}</span></div>"

  typeaheadCtrl.on 'typeahead:asyncrequest', (event, selection) ->
    typed_input = $('.twitter-typeahead').typeahead('val')
    start_time = new Date().getTime()

  typeaheadCtrl.on 'typeahead:asyncreceive', (event, selection) ->
    typed_input = $('.twitter-typeahead').typeahead('val')
    request_time = new Date().getTime() - start_time  # request time in [ms]

  typeaheadCtrl.on 'typeahead:change', (event, selection) ->
    typed_input = $('.twitter-typeahead').typeahead('val')

  # Retrieves the fachinfo, the URL should be of the form /fi/gtin/
  typeaheadCtrl.on 'typeahead:selected', (event, selection) ->
    $.ajax(jsRoutes.controllers.MainController.getFachinfo(language, selection.id))
    .done (response) ->
      window.location.assign '/' + language + '/fi/gtin/' + selection.eancode
      console.log selection.id + ' -> ' + selection.title + ' with language = ' + language
    .fail (jqHXR, textStatus) ->
      alert('ajax error')

  # Detect list related key up and key down events
  typeaheadCtrl.on 'typeahead:cursorchange', (event, selection) ->
    typed_input = $('.twitter-typeahead').typeahead('val')
    $('.twitter-typeahead').val(typed_input)

  # Detect click on search field
  $('#search-field').on 'click', ->
    $('search-field').attr 'value', ''
    $('.twitter-typeahead').typeahead('val', '')
    search_query = setSearchQuery(language, search_type)
    console.log(search_query)
    $('#fachinfo-id').replaceWith ''
    $('#section-ids').replaceWith ''

  setSearchType = (type) ->
    search_type = type
    localStorage.setItem 'search-type', type
    console.log typed_input
    $('.twitter-typeahead').typeahead('val', '').typeahead('val', typed_input)

  # Detect click events on filters
  $('#article-button').on 'click', ->
    setSearchType(1)

  $('#owner-button').on 'click', ->
    setSearchType(2)

  $('#substance-button').on 'click', ->
    setSearchType(3)

  $('#regnr-button').on 'click', ->
    setSearchType(4)

  $('#therapy-button').on 'click', ->
    setSearchType(5)
